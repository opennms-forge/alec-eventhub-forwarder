FROM eclipse-temurin:11
RUN mkdir /opt/alec-eventhub-forwarder
COPY build/install/alec-eventhub-forwarder /opt/alec-eventhub-forwarder
EXPOSE 50051

ENTRYPOINT /opt/alec-eventhub-forwarder/bin/alec-server
