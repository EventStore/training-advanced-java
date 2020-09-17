package com.eventstore.scheduling.eventsourcing;

import lombok.Data;
import lombok.NonNull;

@Data
public class SnapshotEnvelope {
  private final @NonNull Object snapshot;
  private final @NonNull Version version;
}
