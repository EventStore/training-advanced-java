package com.eventstore.scheduling.domain.writemodel.doctorday.state;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduleArchived;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class Cancelled extends State {
  private final @NonNull DayId dayId;

  @Override
  public State apply(Event event) {
    if (event instanceof DayScheduleArchived) {
      return new Archived();
    }
    throw new InvalidStateTransition(this, event);
  }
}
