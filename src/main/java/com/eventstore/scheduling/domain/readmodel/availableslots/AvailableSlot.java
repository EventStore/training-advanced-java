package com.eventstore.scheduling.domain.readmodel.availableslots;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AvailableSlot {
  private final @NonNull DayId dayId;
  private final @NonNull SlotId slotId;
  private final @NonNull LocalDate date;
  private final @NonNull LocalTime time;
  private final @NonNull String duration;
}
