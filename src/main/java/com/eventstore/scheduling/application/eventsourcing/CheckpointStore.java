package com.eventstore.scheduling.application.eventsourcing;

import io.vavr.control.Option;

public interface CheckpointStore {
  Option<Checkpoint> read(SubscriptionId id);

  void write(SubscriptionId id, Checkpoint checkpoint);
}
