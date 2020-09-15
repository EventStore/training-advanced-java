package com.eventstore.scheduling.domain.writemodel.doctorday.command;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.DayAlreadyScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.DayScheduleAlreadyArchived;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.DayScheduleAlreadyCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.*;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.Data;
import lombok.NonNull;
import lombok.val;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleDay implements Command {
  private final @NonNull DoctorId doctorId;
  private final @NonNull LocalDate date;
  private final @NonNull List<ScheduleSlot> slots;

  @Override
  public Either<Error, List<Event>> apply(State state, IdGenerator idGenerator) {
    if (state instanceof Unscheduled) {
      val dayId = new DayId(doctorId, date);
      val dayScheduled = new DayScheduled(dayId, doctorId, date);
      List<Event> slotsScheduled =
          slots.map(
              (slot) ->
                  new SlotScheduled(
                      SlotId.create(idGenerator),
                      dayId,
                      LocalDateTime.of(date, slot.getStartTime()),
                      slot.getDuration()));
      return Either.right(slotsScheduled.prepend(dayScheduled));
    }
    if (state instanceof Archived) {
      return Either.left(new DayScheduleAlreadyArchived());
    }
    if (state instanceof Cancelled) {
      return Either.left(new DayScheduleAlreadyCancelled());
    }
    if (state instanceof Scheduled) {
      return Either.left(new DayAlreadyScheduled());
    }
    return null;
  }
}
