package com.eventstore.scheduling.domain.doctorday.command;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DoctorId;
import com.eventstore.scheduling.eventsourcing.Command;
import io.vavr.collection.List;
import lombok.NonNull;

import java.time.LocalDate;

public record ScheduleDay(
    @NonNull DoctorId doctorId,
    @NonNull LocalDate date,
    @NonNull List<ScheduleSlot> slots
) implements Command
{
    public DayId dayId() {
        return new DayId(doctorId, date);
    }
}
