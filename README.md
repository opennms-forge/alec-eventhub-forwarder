# alec-eventhub-forwarder

Simple java server for converting alec grpc messages to event-bus events.

## Build

    gradlew clean installDist

## Run

Put event hub configuration and JWT signing key in the runtime.properties file and run:

    ./build/install/alec-eventhub-forwarder/bin/alec-server
