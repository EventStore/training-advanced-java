package com.eventstore.scheduling.domain.writemodel.doctorday.command;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.DayNotScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.DayScheduleAlreadyArchived;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduleArchived;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.*;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.Data;

@Data
public class Archive implements Command {
  @Override
  public Either<Error, List<Event>> apply(State state, IdGenerator idGenerator) {
    if (state instanceof Unscheduled) {
      return Either.left(new DayNotScheduled());
    }
    if (state instanceof Archived) {
      return Either.left(new DayScheduleAlreadyArchived());
    }
    if (state instanceof Cancelled) {
      return Either.right(List.of(new DayScheduleArchived(((Cancelled) state).getDayId())));
    }
    if (state instanceof Scheduled) {
      return Either.right(List.of(new DayScheduleArchived(((Scheduled) state).getDayId())));
    }
    return null;
  }
}
