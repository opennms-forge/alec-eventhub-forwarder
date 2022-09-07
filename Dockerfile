FROM eclipse-temurin:11
RUN mkdir /opt/alec-eventhub-forwarder
RUN mkdir /opt/alec-eventhub-forwarder/bin
RUN mkdir /opt/alec-eventhub-forwarder/lib
COPY src/main/scripts/alec-server /opt/alec-eventhub-forwarder/bin/alec-server
COPY target/alec-eventhub-forwarder-0.1.0-SNAPSHOT-jar-with-dependencies.jar /opt/alec-eventhub-forwarder/lib/alec-eventhub-forwarder-0.1.0-SNAPSHOT-jar-with-dependencies.jar
EXPOSE 50051

ENTRYPOINT /opt/alec-eventhub-forwarder/bin/alec-server
