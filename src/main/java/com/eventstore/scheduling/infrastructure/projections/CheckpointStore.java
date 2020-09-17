package com.eventstore.scheduling.infrastructure.projections;

import com.eventstore.scheduling.eventsourcing.SubscriptionId;
import com.eventstore.scheduling.infrastructure.eventstore.Checkpoint;
import io.vavr.control.Option;

public interface CheckpointStore {
  Option<Checkpoint> getCheckpoint();

  void storeCheckpoint(Checkpoint checkpoint);
}
