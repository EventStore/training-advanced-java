package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.dbclient.Subscription;
import com.eventstore.scheduling.domain.writemodel.State;

import java.util.concurrent.CompletableFuture;

public interface CommandStore {
  void send(Object command, CommandMetadata metadata);

  <C, E, Er, S extends State<S, E>> CompletableFuture<Subscription> subscribe(
          CommandHandler<C, E, Er, S> commandHandler, CheckpointStore checkpointStore);
}
