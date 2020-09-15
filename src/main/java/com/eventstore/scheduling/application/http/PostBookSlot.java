package com.eventstore.scheduling.application.http;

import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.BookSlot;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.PatientId;
import lombok.Data;
import lombok.NonNull;

@Data
public class PostBookSlot {
  private final @NonNull String slotId;
  private final @NonNull String patientId;

  public BookSlot toCommand() {
    return new BookSlot(new SlotId(slotId), new PatientId(patientId));
  }
}
