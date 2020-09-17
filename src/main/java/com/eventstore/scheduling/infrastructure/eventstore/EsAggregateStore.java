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
        eventStore.appendSnapshot("doctorday-" + aggregate.getId(), aggregate.getVersion() + changes.length(), snapshotAggregate.getSnapshot());
      }
    }
  }

  @Override
  public <T extends AggregateRoot> T load(T aggregate, String aggregateId) {

    var version = -1L;
    if (aggregate instanceof AggregateRootSnapshot) {
      version = loadSnapshot((AggregateRootSnapshot) aggregate, aggregateId, version);
    }

    val events = eventStore.loadEvents("doctorday-" + aggregateId, Some(version));

    aggregate.load(events);
    aggregate.clearChanges();

    return aggregate;
  }

  private <T extends AggregateRoot> long loadSnapshot(AggregateRootSnapshot aggregate, String aggregateId, long version) {
    // Load snapshot from the store
    // If there is one then load it into the aggregate
    // Return next expected version
    return -1L;
  }
}
