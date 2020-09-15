package com.eventstore.scheduling.domain.writemodel.doctorday.command;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.*;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.*;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.Data;
import lombok.NonNull;
import lombok.val;

@Data
public class BookSlot implements Command {
  private final @NonNull SlotId slotId;
  private final @NonNull PatientId patientId;

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
      if (scheduled.hasAvaliableSlot(slotId)) {
        return Either.right(List.of(new SlotBooked(slotId, patientId)));
      } else if (scheduled.hasSlot(slotId)) {
        return Either.left(new SlotAlreadyBooked());
      } else {
        return Either.left(new SlotNotScheduled());
      }
    }
    return null;
  }
}
