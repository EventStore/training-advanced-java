package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.scheduling.application.eventsourcing.EventStoreClient;
import com.eventstore.scheduling.application.eventsourcing.SnapshotEnvelope;
import com.eventstore.scheduling.application.eventsourcing.SnapshotMetadata;
import com.eventstore.scheduling.application.eventsourcing.SnapshotStore;
import com.eventstore.scheduling.domain.writemodel.AggregateId;
import io.vavr.collection.List;
import io.vavr.control.Option;

public class EsSnapshotStore implements SnapshotStore {
  private final EventStoreClient<SnapshotMetadata> eventStoreClient;

  public EsSnapshotStore(EventStoreClient<SnapshotMetadata> eventStoreClient) {
    this.eventStoreClient = eventStoreClient;
  }

  @Override
  public Option<SnapshotEnvelope> read(AggregateId id) {
    return eventStoreClient
        .readLastFromStream("snapshot-" + id.toString())
        .map(message -> new SnapshotEnvelope(message.getData(), message.getMetadata()));
  }

  @Override
  public void write(AggregateId id, Object snapshot, SnapshotMetadata metadata) {
    eventStoreClient.appendToStream("snapshot-" + id.toString(), List.of(snapshot), metadata);
  }
}
