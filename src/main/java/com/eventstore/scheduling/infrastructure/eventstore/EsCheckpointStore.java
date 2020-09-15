package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.scheduling.application.eventsourcing.Checkpoint;
import com.eventstore.scheduling.application.eventsourcing.CheckpointStore;
import com.eventstore.scheduling.application.eventsourcing.EventStoreClient;
import com.eventstore.scheduling.application.eventsourcing.SubscriptionId;
import io.vavr.collection.List;
import io.vavr.control.Option;

public class EsCheckpointStore implements CheckpointStore {
  private final EventStoreClient<Object> eventStoreClient;

  public EsCheckpointStore(EventStoreClient<Object> eventStoreClient) {
    this.eventStoreClient = eventStoreClient;
  }

  @Override
  public Option<Checkpoint> read(SubscriptionId id) {
    return eventStoreClient
        .readLastFromStream(getStreamId(id))
        .map(message -> (Checkpoint) message.getData());
  }

  @Override
  public void write(SubscriptionId id, Checkpoint checkpoint) {
    eventStoreClient.appendToStream(getStreamId(id), List.of(checkpoint), new Object());
  }

  private String getStreamId(SubscriptionId id) {
    return "checkpoint-" + id.getValue();
  }
}
