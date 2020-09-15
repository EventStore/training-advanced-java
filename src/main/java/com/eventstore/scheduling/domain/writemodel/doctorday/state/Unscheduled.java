package com.eventstore.scheduling.domain.writemodel.doctorday.state;

import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import io.vavr.collection.List;
import lombok.val;

public class Unscheduled extends State {
  @Override
  public State apply(Event event) {
    if (event instanceof DayScheduled) {
      val dayScheduled = (DayScheduled) event;
      return new Scheduled(
          dayScheduled.getDayId(), dayScheduled.getDate(), new Slots(List.empty()));
    }
    throw new InvalidStateTransition(this, event);
  }
}
