package com.eventstore.scheduling.eventsourcing;

import lombok.Data;
import lombok.NonNull;

@Data
public class CommandMetadata {
  private final @NonNull CorrelationId correlationId;
  private final @NonNull CausationId causationId;

  public static CommandMetadata of(String correlationId, String causationId) {
    return new CommandMetadata(new CorrelationId(correlationId), new CausationId(causationId));
  }
}
