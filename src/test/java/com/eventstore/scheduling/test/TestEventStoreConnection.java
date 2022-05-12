package com.eventstore.scheduling.test;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;

public interface TestEventStoreConnection {
  EventStoreDBClientSettings setts = EventStoreDBConnectionString.parseOrThrow("esdb://localhost:2113?tls=false");
  EventStoreDBClient client = EventStoreDBClient.create(setts);
}
