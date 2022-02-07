package com.eventstore.scheduling.domain.readmodel.availableslots;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailableSlot(
    @NonNull DayId dayId,
    @NonNull SlotId slotId,
    @NonNull LocalDate date,
    @NonNull LocalTime time,
    @NonNull String duration
) {}
