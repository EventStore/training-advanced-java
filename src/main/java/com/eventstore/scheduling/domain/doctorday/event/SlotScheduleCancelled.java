package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.eventsourcing.Event;
import lombok.Data;
import lombok.NonNull;

@Data
public class SlotScheduleCancelled implements Event {
  private final @NonNull DayId dayId;
  private final @NonNull SlotId slotId;
}
