package com.eventstore.scheduling.test;

import com.eventstore.dbclient.StreamsClient;
import com.eventstore.dbclient.Timeouts;
import com.eventstore.dbclient.UserCredentials;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;

public interface TestEventStoreConnection {
  UserCredentials credentials = new UserCredentials("admin", "changeit");
  StreamsClient streamsClient =
      new StreamsClient("localhost", 2113, credentials, Timeouts.DEFAULT, getClientSslContext());

  private static SslContext getClientSslContext() {
    try {
      return GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    } catch (SSLException ignored) {
    }
    return null;
  }
}
