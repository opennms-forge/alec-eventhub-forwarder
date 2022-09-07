# alec-eventhub-forwarder

Simple java server for converting alec grpc messages to event-bus events.

## Azure infrastructure

The storage account and event hub resources are created by running the terraform in the tf directory. This is not run as part of the build and must be done separately.

## Build

    gradlew clean installDist

## Run

Put event hub configuration and JWT signing key in the runtime.properties file and run:

    ./build/install/alec-eventhub-forwarder/bin/alec-server

## Container

A dockerfile is provided to generate a container with the java server. This is run by the github action, but the container is not currently exported.
