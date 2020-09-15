package com.eventstore.scheduling.domain.writemodel.doctorday.event;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
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
