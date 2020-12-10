package com.eventstore.scheduling.test;

import com.eventstore.dbclient.*;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;

public interface TestEventStoreConnection {
  ClientSettings setts = ConnectionString.parseOrThrow("esdb://admin:changeit@localhost:2113?tlsVerifyCert=false&tls=false");
  Streams client = Client.create(setts).streams();
}
