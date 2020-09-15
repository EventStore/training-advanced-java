package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.Subscription;
import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.writemodel.State;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EsCommandStore implements CommandStore {

  private final String streamId;
  private final EsEventStoreClient<CommandMetadata> eventStore;

  public EsCommandStore(String streamId, EsEventStoreClient<CommandMetadata> eventStore) {
    this.streamId = streamId;
    this.eventStore = eventStore;
  }

  @Override
  public void send(Object command, CommandMetadata metadata) {
    eventStore.appendToStream(streamId, List.of(command), metadata);
  }

  @Override
  public <C, E, Er, S extends State<S, E>> CompletableFuture<Subscription> subscribe(
      CommandHandler<C, E, Er, S> commandHandler, CheckpointStore checkpointStore) {
    return new PersistentSubscriptionFactory<>(eventStore, checkpointStore)
        .subscribeToAStream(
            new SubscriptionId("async_command_handler-" + streamId),
            streamId,
            new MessageHandler<CommandMetadata>() {
              @Override
              public void handle(
                  Object message,
                  CommandMetadata metadata,
                  UUID messageId,
                  Instant occurredAt,
                  Version position,
                  Option<Version> streamPosition) {
                commandHandler.handle((C) message, metadata);
              }
            });
  }
}
