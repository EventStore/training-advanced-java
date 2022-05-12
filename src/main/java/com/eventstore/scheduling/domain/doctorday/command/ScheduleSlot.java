package com.eventstore.scheduling.domain.doctorday.command;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.eventsourcing.Command;
import lombok.NonNull;

import java.time.Duration;
import java.time.LocalTime;

public record ScheduleSlot(
    @NonNull DayId dayId,
    @NonNull LocalTime startTime,
    @NonNull Duration duration
) implements Command {}
