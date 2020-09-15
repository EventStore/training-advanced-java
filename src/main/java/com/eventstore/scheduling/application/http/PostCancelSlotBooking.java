package com.eventstore.scheduling.application.http;

import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.CancelSlotBooking;
import lombok.Data;
import lombok.NonNull;

@Data
public class PostCancelSlotBooking {
  private final @NonNull String slotId;
  private final @NonNull String reason;

  public CancelSlotBooking toCommand() {
    return new CancelSlotBooking(new SlotId(slotId), reason);
  }
}
