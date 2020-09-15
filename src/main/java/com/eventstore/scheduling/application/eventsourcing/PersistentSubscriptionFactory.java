package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.dbclient.Subscription;
import io.vavr.control.Option;
import lombok.val;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PersistentSubscriptionFactory<M> {
  private final EventStoreClient<M> eventStoreClient;
  private final CheckpointStore checkpointStore;

  public PersistentSubscriptionFactory(
      EventStoreClient<M> eventStoreClient, CheckpointStore checkpointStore) {
    this.eventStoreClient = eventStoreClient;
    this.checkpointStore = checkpointStore;
  }

  public CompletableFuture<Subscription> subscribeToAStream(
      SubscriptionId subscriptionId, String streamId, MessageHandler<M> handler) {

    MessageHandler<M> checkpointingHandler =
        new CheckpointingHandler<>(subscriptionId, handler, checkpointStore);

    val startCheckpoint = checkpointStore.read(subscriptionId);

    System.out.println(
        "Starting "
            + subscriptionId.getValue()
            + " @ "
            + streamId
            + " subscription from checkpoint "
            + startCheckpoint.toString());

    return eventStoreClient.subscribeToAStream(streamId, startCheckpoint, checkpointingHandler);
  }

  private static class CheckpointingHandler<M> implements MessageHandler<M> {
    private final SubscriptionId subscriptionId;
    private final MessageHandler<M> decorated;
    private final CheckpointStore checkpointStore;

    public CheckpointingHandler(
        SubscriptionId subscriptionId,
        MessageHandler<M> decorated,
        CheckpointStore checkpointStore) {
      this.subscriptionId = subscriptionId;
      this.decorated = decorated;
      this.checkpointStore = checkpointStore;
    }

    @Override
    public void handle(
        Object message,
        M metadata,
        UUID messageId,
        Instant occurredAt,
        Version position,
        Option<Version> streamPosition) {
      decorated.handle(message, metadata, messageId, occurredAt, position, streamPosition);
      Checkpoint checkpoint = new Checkpoint(streamPosition.getOrElse(position).getValue());
      checkpointStore.write(subscriptionId, checkpoint);

      System.out.println(
          "Committed "
              + subscriptionId.toString()
              + " subscription checkpoint "
              + checkpoint.toString());
    }
  }
}
