package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.eventsourcing.Event;
import lombok.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;

public record SlotScheduled (
    @NonNull SlotId slotId,
    @NonNull DayId dayId,
    @NonNull LocalDateTime startDateTime,
    @NonNull Duration duration
) implements Event {}
