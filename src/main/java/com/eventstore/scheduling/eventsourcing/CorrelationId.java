package com.eventstore.scheduling.eventsourcing;

import com.eventstore.scheduling.domain.service.IdGenerator;
import lombok.NonNull;

public record CorrelationId(
    @NonNull String value
)
{
    public static CorrelationId create(IdGenerator idGenerator) {
      return new CorrelationId(idGenerator.nextUuid().toString());
    }
}
