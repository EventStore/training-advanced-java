package com.eventstore.scheduling.controllers;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.command.CancelSlotBooking;
import lombok.Data;
import lombok.NonNull;

@Data
public class PostCancelSlotBooking {
  private final @NonNull String slotId;
  private final @NonNull String reason;

  public CancelSlotBooking toCommand(DayId dayId) {
    return new CancelSlotBooking(dayId, new SlotId(slotId), reason);
  }
}
