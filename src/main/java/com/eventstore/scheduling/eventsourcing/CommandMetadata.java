package com.eventstore.scheduling.eventsourcing;

import lombok.NonNull;

public record CommandMetadata(
    @NonNull CorrelationId correlationId,
    @NonNull CausationId causationId
)
{
   public static CommandMetadata of(String correlationId, String causationId) {
      return new CommandMetadata(new CorrelationId(correlationId), new CausationId(causationId));
   }
}
