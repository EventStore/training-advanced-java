package com.eventstore.scheduling.domain.doctorday.command;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.PatientId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.eventsourcing.Command;
import lombok.Data;
import lombok.NonNull;

@Data
public class BookSlot implements Command {
  private final @NonNull DayId dayId;
  private final @NonNull SlotId slotId;
  private final @NonNull PatientId patientId;
}
