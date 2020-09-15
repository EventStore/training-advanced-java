package com.eventstore.scheduling.domain.writemodel.doctorday.event;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import lombok.Data;
import lombok.NonNull;

@Data
public class DayScheduleArchived implements Event {
  private final @NonNull DayId dayId;
}
