package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.eventsourcing.Event;
import lombok.Data;
import lombok.NonNull;

@Data
public class DayScheduleArchived implements Event {
  private final @NonNull DayId dayId;
}
