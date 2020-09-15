package com.eventstore.scheduling.domain.writemodel.doctorday.event;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import lombok.Data;
import lombok.NonNull;

@Data
public class DayScheduleCancelled implements Event {
  private final @NonNull DayId dayId;
  private final @NonNull String reason;
}
