package com.eventstore.scheduling.application.eventsourcing;

import lombok.Data;
import lombok.NonNull;

@Data
public class SnapshotEnvelope {
  private final @NonNull Object snapshot;
  private final @NonNull SnapshotMetadata metadata;
}
