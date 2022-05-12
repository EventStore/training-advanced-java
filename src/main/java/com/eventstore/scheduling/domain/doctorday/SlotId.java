package com.eventstore.scheduling.domain.doctorday;

import com.eventstore.scheduling.domain.service.IdGenerator;
import lombok.Data;
import lombok.NonNull;

public record SlotId(
    @NonNull String value
)
{
    public static SlotId create(IdGenerator idGenerator) {
        return new SlotId(idGenerator.nextUuid().toString());
    }
}
