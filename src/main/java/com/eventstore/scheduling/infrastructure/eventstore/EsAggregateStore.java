package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.scheduling.eventsourcing.*;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;

import static io.vavr.API.Some;

public class EsAggregateStore implements AggregateStore {
  private final EventStore eventStore;
  private final int snapshotThreshold;

  public EsAggregateStore(EventStore eventStore, int snapshotThreshold) {
    this.eventStore = eventStore;
    this.snapshotThreshold = snapshotThreshold;
  }

  @Override
  public <T extends AggregateRoot> void save(T aggregate, CommandMetadata metadata) {
    val changes = aggregate.getChanges();
    eventStore.appendEvents("doctorday-" + aggregate.getId(), aggregate.getVersion(), metadata, changes);

    if (aggregate instanceof AggregateRootSnapshot) {
      val snapshotAggregate = (AggregateRootSnapshot) aggregate;
      if((snapshotAggregate.getVersion() + changes.length() + 1) - snapshotAggregate.getSnapshotVersion() >= snapshotThreshold) {
        eventStore.appendSnapshot(
                "doctorday-" + aggregate.getId(),
                aggregate.getVersion() + changes.length(),
                snapshotAggregate.getSnapshot(),
                metadata
        );
      }
    }
  }

  @Override
  public <T extends AggregateRoot> T load(T aggregate, String aggregateId) {
    long version = -1L;
    if (aggregate instanceof AggregateRootSnapshot) {
      val snapshotEnvelope = eventStore.loadSnapshot("doctorday-" + aggregateId);
      if (snapshotEnvelope != null) {
        val snapshotAggregate = (AggregateRootSnapshot) aggregate;
        snapshotAggregate.loadSnapshot(snapshotEnvelope.getSnapshot(), snapshotEnvelope.getVersion().getValue());
        version = snapshotEnvelope.getVersion().getValue() + 1L;
      }
    }

    val events = eventStore.loadEvents("doctorday-" + aggregateId, Some(version));

    aggregate.load(events);
    aggregate.clearChanges();

    return aggregate;
  }
}
