package com.eventstore.scheduling.test;

import com.eventstore.dbclient.*;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;

public interface TestEventStoreConnection {
  EventStoreDBClientSettings setts = EventStoreDBConnectionString.parseOrThrow("esdb://admin:changeit@localhost:2113?tlsVerifyCert=false&tls=false");
  EventStoreDBClient client = EventStoreDBClient.create(setts);
}
