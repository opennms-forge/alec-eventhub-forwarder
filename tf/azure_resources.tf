terraform {
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
    }
  }
}

provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "ResourceGroup" {
  name = local.ResourceGroup
  location = "eastus"
}

resource "azurerm_storage_account" "StorageAccount" {
  name = local.StorageAccount
  resource_group_name = azurerm_resource_group.ResourceGroup.name
  location = azurerm_resource_group.ResourceGroup.location
  account_tier = "Standard"
  account_replication_type = "LRS"
  account_kind = "StorageV2"
  is_hns_enabled = true
}

resource "azurerm_storage_data_lake_gen2_filesystem" "gen2fs" {
  name = local.ContainerName
  storage_account_id = azurerm_storage_account.StorageAccount.id
}

resource "azurerm_eventhub_namespace" "ehnamespace" {
  name = local.EventHubNamespace
  location = azurerm_resource_group.ResourceGroup.location
  resource_group_name = azurerm_resource_group.ResourceGroup.name
  sku = "Standard"
  capacity = 1

  tags = {
    environment = "poc"
  }
}

resource "azurerm_eventhub" "eventhub" {
  name = local.EventHubName
  namespace_name = azurerm_eventhub_namespace.ehnamespace.name
  resource_group_name = azurerm_resource_group.ResourceGroup.name
  partition_count = 1
  message_retention = 1
  capture_description {
    enabled = true
    encoding = "Avro"
    skip_empty_archives = true
    destination {
      name = "EventHubArchive.AzureBlockBlob"
      archive_name_format = "{Namespace}/{EventHub}/{PartitionId}/{Year}/{Month}/{Day}/{Hour}/{Minute}/{Second}"
      blob_container_name = azurerm_storage_data_lake_gen2_filesystem.gen2fs.name
      storage_account_id = azurerm_storage_account.StorageAccount.id
    }
  }
}

resource "azurerm_eventhub_authorization_rule" ehauthrule {
  name = "javaaccess"
  namespace_name = azurerm_eventhub_namespace.ehnamespace.name
  eventhub_name = azurerm_eventhub.eventhub.name
  resource_group_name = azurerm_resource_group.ResourceGroup.name
  listen = true
  send = true
  manage = false
}

resource "local_file" "runtimeproperties" {
  content = join("\n",
       ["apiVersion: v1",
        "kind: Secret",
        "metadata:",
        "  name: az-secrets",
        "data:",
        join("", ["  EH_NAME: ", base64encode(azurerm_eventhub.eventhub.name)]),
        join("", ["  EH_CONNECTION_STRING: ", base64encode(azurerm_eventhub_authorization_rule.ehauthrule.primary_connection_string)]),
        ""
       ])
  filename = "k8s-azsecrets.yaml"
}

