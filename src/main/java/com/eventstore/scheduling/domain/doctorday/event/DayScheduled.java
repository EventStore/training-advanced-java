package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DoctorId;
import com.eventstore.scheduling.eventsourcing.Event;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

public record DayScheduled(
    @NonNull DayId dayId,
    @NonNull DoctorId doctorId,
    @NonNull LocalDate date
) implements Event {}
