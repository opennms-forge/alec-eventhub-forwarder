# alec-eventhub-forwarder

Simple java server for converting alec grpc messages to event-bus events.

## Azure infrastructure

The storage account and event hub resources are created by running the terraform in the tf directory. This is not run as part of the build and must be done separately.

    cd tf
    terraform apply

## Infrastructure and prerequisites

Running terraform within the tf directory will generate the event hub and storage account. It will also generate a k8s secret definition for connecting to the event hub.

A JWT signing key must be base64 encoded and put in the following secret definition. Any tokens used to connect with the service must be created using this key.

    vi src/main/resources/k8s-jwtsecret.yaml
    
## Build

To just build the java code:

    mvn clean install

To build and deploy, assuming kubectl is setup and the terraform has already been run:

    skaffold dev
