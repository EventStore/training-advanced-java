package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.scheduling.eventsourcing.*;
import lombok.val;

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
    eventStore.appendEvents("doctorday-%s".formatted(aggregate.getId()), aggregate.getVersion(), metadata, changes);

    if (aggregate instanceof AggregateRootSnapshot) {
      val snapshotAggregate = (AggregateRootSnapshot) aggregate;
      if((snapshotAggregate.getVersion() + changes.length() + 1) - snapshotAggregate.getSnapshotVersion() >= snapshotThreshold) {
        eventStore.appendSnapshot(
                "doctorday-%s".formatted(aggregate.getId()),
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
      val snapshotEnvelope = eventStore.loadSnapshot("doctorday-%s".formatted(aggregateId));
      if (snapshotEnvelope != null) {
        val snapshotAggregate = (AggregateRootSnapshot) aggregate;
        snapshotAggregate.loadSnapshot(snapshotEnvelope.snapshot(), snapshotEnvelope.version().value());
        version = snapshotEnvelope.version().value() + 1L;
      }
    }

    val events = eventStore.loadEvents("doctorday-%s".formatted(aggregateId), Some(version));

    aggregate.load(events);
    aggregate.clearChanges();

    return aggregate;
  }
}
