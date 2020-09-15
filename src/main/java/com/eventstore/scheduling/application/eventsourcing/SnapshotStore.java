package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.scheduling.domain.writemodel.AggregateId;
import io.vavr.control.Option;

public interface SnapshotStore {
  Option<SnapshotEnvelope> read(AggregateId id);

  void write(AggregateId id, Object snapshot, SnapshotMetadata metadata);
}
