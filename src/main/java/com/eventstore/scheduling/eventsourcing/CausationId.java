package com.eventstore.scheduling.eventsourcing;

import com.eventstore.scheduling.domain.service.IdGenerator;
import lombok.NonNull;

public record CausationId(
    @NonNull String value
)
{
    public static CausationId create(IdGenerator idGenerator) {
        return new CausationId(idGenerator.nextUuid().toString());
    }
}
