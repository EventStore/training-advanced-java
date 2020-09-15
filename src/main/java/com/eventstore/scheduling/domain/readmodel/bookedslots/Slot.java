package com.eventstore.scheduling.domain.readmodel.bookedslots;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import lombok.Data;
import lombok.NonNull;

import java.time.Month;

@Data
public class Slot {
  private final @NonNull SlotId slotId;
  private final @NonNull DayId dayId;
  private final @NonNull Month month;
}
