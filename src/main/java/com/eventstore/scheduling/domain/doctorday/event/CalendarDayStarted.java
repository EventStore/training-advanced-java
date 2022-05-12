package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.eventsourcing.Event;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

public record CalendarDayStarted(
    @NonNull LocalDate date
) implements Event {}
