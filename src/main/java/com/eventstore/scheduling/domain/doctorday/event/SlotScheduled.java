package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.eventsourcing.Event;
import lombok.Data;
import lombok.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
public class SlotScheduled implements Event {
  private final @NonNull SlotId slotId;
  private final @NonNull DayId dayId;
  private final @NonNull LocalDateTime startDateTime;
  private final @NonNull Duration duration;
}
