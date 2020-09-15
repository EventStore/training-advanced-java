package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.scheduling.domain.writemodel.AggregateId;
import lombok.Data;
import lombok.NonNull;

@Data
public class CommandMetadata {
  private final @NonNull CorrelationId correlationId;
  private final @NonNull CausationId causationId;
  private final @NonNull AggregateId aggregateId;
}
