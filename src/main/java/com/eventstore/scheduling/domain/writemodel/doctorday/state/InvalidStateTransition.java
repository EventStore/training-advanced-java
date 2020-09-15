package com.eventstore.scheduling.domain.writemodel.doctorday.state;

import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@EqualsAndHashCode(callSuper = false)
public class InvalidStateTransition extends RuntimeException {
  private final @NonNull State state;
  private final @NonNull Event event;

  @Override
  public String getMessage() {
    return "Invalid state transition. " + state + " is not able to handle " + event;
  }
}
