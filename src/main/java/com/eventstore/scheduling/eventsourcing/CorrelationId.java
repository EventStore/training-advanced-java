package com.eventstore.scheduling.eventsourcing;

import com.eventstore.scheduling.domain.service.IdGenerator;
import lombok.Data;
import lombok.NonNull;

@Data
public class CorrelationId {
  private final @NonNull String value;

  public static CorrelationId create(IdGenerator idGenerator) {
    return new CorrelationId(idGenerator.nextUuid().toString());
  }
}
