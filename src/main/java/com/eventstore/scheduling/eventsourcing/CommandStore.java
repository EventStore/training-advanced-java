package com.eventstore.scheduling.eventsourcing;

import com.eventstore.dbclient.Subscription;
import com.eventstore.scheduling.infrastructure.projections.CheckpointStore;

import java.util.concurrent.CompletableFuture;

public interface CommandStore {
  void send(Object command, CommandMetadata metadata);

  void start();
}
