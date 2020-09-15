package com.eventstore.scheduling.domain.writemodel.doctorday.event;

import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.PatientId;
import lombok.Data;
import lombok.NonNull;

@Data
public class SlotBooked implements Event {
  private final @NonNull SlotId slotId;
  private final @NonNull PatientId patientId;
}
