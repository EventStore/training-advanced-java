package com.eventstore.scheduling.application.eventsourcing;

import io.vavr.control.Option;
import lombok.Data;
import lombok.NonNull;

@Data
public class EventMetadata {
  private final @NonNull CorrelationId correlationId;
  private final @NonNull CausationId causationId;
  private final @NonNull Option<String> replayed;
}
