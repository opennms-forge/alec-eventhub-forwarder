/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.aleccloud;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import org.opennms.aleccloud.SituationSetProtos.*;

import com.google.protobuf.Empty;
import com.google.protobuf.util.JsonFormat;
import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.io.FileInputStream;

import com.azure.messaging.eventhubs.*;

public class AlecCloudServer {
  private static final Logger logger = Logger.getLogger(AlecCloudServer.class.getName());

  private static String connectionString = "";
  private static String eventHubName = "";
  private Server server;

  private void start() throws IOException {
    Properties props = new Properties();
    props.load(new FileInputStream("runtime.properties"));

    /* The port on which the server should run */
    int port = Integer.valueOf(props.getProperty("GRPC_PORT"));
    connectionString = props.getProperty("EH_CONNECTION_STRING");
    eventHubName = props.getProperty("EH_NAME");

    logger.info("key is " + props.getProperty("JWT_SIGNING_KEY"));
    server = ServerBuilder.forPort(port)
        .addService(new AlecCollectionServiceImpl())
        .intercept(new AuthorizationServerInterceptor(props.getProperty("JWT_SIGNING_KEY")))
        .build()
        .start();
    logger.info("Server started, listening on " + port);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        try {
          AlecCloudServer.this.stop();
        } catch (InterruptedException e) {
          e.printStackTrace(System.err);
        }
        System.err.println("*** server shut down");
      }
    });
  }

  private void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  private void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    final AlecCloudServer server = new AlecCloudServer();
    server.start();
    server.blockUntilShutdown();
  }

  static class AlecCollectionServiceImpl extends org.opennms.aleccloud.AlecCollectionServiceGrpc.AlecCollectionServiceImplBase {


    @Override
    public void sendSituations(SituationSet request,
                           StreamObserver<Empty> responseObserver) {
      Empty reply = Empty.newBuilder().build();

      try {
        String json = JsonFormat.printer().print(request);
        publishEvent(json);
      } catch (InvalidProtocolBufferException ex) {
        System.err.println("Exception converting to json: " + ex.getLocalizedMessage());
      }
      responseObserver.onNext(reply);
      responseObserver.onCompleted();
    }

    public void publishEvent(String event) {
      EventHubProducerClient producer = new EventHubClientBuilder()
              .connectionString(connectionString, eventHubName)
              .buildProducerClient();

      List<EventData> allEvents = Arrays.asList(new EventData(event));

      EventDataBatch eventDataBatch = producer.createBatch();
      for (EventData eventData : allEvents) {
        if (!eventDataBatch.tryAdd(eventData)) {
          producer.send(eventDataBatch);
          eventDataBatch = producer.createBatch();

          eventDataBatch.tryAdd(eventData);
        }
      }
      if (eventDataBatch.getCount() > 0) {
        producer.send(eventDataBatch);
      }
      producer.close();
    }
  }

}
