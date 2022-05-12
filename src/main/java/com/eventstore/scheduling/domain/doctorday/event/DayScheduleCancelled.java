package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.eventsourcing.Event;
import lombok.NonNull;

public record DayScheduleCancelled(
    @NonNull DayId dayId,
    @NonNull String reason
) implements Event {}
