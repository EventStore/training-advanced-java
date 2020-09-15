package com.eventstore.scheduling.application.eventsourcing;

import lombok.Data;
import lombok.NonNull;

@Data
public class SnapshotMetadata {
  private final @NonNull CorrelationId correlationId;
  private final @NonNull CausationId causationId;
  private final @NonNull Version version;
}
