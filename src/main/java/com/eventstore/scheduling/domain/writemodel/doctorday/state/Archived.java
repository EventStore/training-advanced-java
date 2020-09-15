package com.eventstore.scheduling.domain.writemodel.doctorday.state;

import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Archived extends State {
  @Override
  public State apply(Event event) {
    throw new InvalidStateTransition(this, event);
  }
}
