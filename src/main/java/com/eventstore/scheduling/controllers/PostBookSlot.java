package com.eventstore.scheduling.controllers;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.command.BookSlot;
import com.eventstore.scheduling.domain.doctorday.PatientId;
import lombok.Data;
import lombok.NonNull;

@Data
public class PostBookSlot {
  private final @NonNull String slotId;
  private final @NonNull String patientId;

  public BookSlot toCommand(DayId dayId) {
    return new BookSlot(dayId, new SlotId(slotId), new PatientId(patientId));
  }
}
