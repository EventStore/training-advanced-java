package com.eventstore.scheduling.domain.doctorday.command;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.PatientId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.eventsourcing.Command;
import lombok.NonNull;

public record BookSlot(
  @NonNull DayId dayId,
  @NonNull SlotId slotId,
  @NonNull PatientId patientId
) implements Command {}
