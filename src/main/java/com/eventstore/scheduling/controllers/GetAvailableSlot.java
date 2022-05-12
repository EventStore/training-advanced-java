package com.eventstore.scheduling.controllers;

import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record GetAvailableSlot(
    @NonNull String dayId,
    @NonNull String slotId,
    @NonNull LocalDate date,
    @NonNull LocalTime time,
    @NonNull String duration
)
{
    public static GetAvailableSlot fromDomain(AvailableSlot availableSlot) {
        return new GetAvailableSlot(
            availableSlot.dayId().value(),
            availableSlot.slotId().value(),
            availableSlot.date(),
            availableSlot.time(),
            availableSlot.duration()
        );
    }
}
