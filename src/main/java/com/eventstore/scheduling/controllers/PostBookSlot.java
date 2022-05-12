package com.eventstore.scheduling.controllers;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.command.BookSlot;
import com.eventstore.scheduling.domain.doctorday.PatientId;
import lombok.NonNull;

public record PostBookSlot(
    @NonNull String slotId,
    @NonNull String patientId
)
{
    public BookSlot toCommand(DayId dayId) {
        return new BookSlot(dayId, new SlotId(slotId), new PatientId(patientId));
    }
}
