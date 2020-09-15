package com.eventstore.scheduling.domain.writemodel.doctorday.command;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.*;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.*;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.Data;
import lombok.NonNull;
import lombok.val;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ScheduleSlot implements Command {
  private final @NonNull LocalTime startTime;
  private final @NonNull Duration duration;

  @Override
  public Either<Error, List<Event>> apply(State state, IdGenerator idGenerator) {
    if (state instanceof Unscheduled) {
      return Either.left(new DayNotScheduled());
    }
    if (state instanceof Archived) {
      return Either.left(new DayScheduleAlreadyArchived());
    }
    if (state instanceof Cancelled) {
      return Either.left(new DayScheduleAlreadyCancelled());
    }
    if (state instanceof Scheduled) {
      val scheduled = (Scheduled) state;
      if (scheduled.doesNotOverlap(startTime, duration)) {
        return Either.right(
            List.of(
                new SlotScheduled(
                    SlotId.create(idGenerator),
                    scheduled.getDayId(),
                    LocalDateTime.of(scheduled.getDate(), startTime),
                    duration)));
      }
      return Either.left(new SlotOverlapped());
    }
    return null;
  }
}
