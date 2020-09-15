package com.eventstore.scheduling.domain.writemodel.doctorday.command;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.DayNotScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.DayScheduleAlreadyArchived;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.DayScheduleAlreadyCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduleCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.*;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.Data;
import lombok.NonNull;
import lombok.val;

@Data
public class CancelDaySchedule implements Command {
  private final @NonNull String reason;

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

      List<Event> cancelledBookings =
          scheduled
              .getBookedSlots()
              .map((slot) -> (new SlotBookingCancelled(slot.getSlotId(), reason)));

      List<Event> cancelledSlots =
          scheduled.getAllSlots().map((slot) -> (new SlotCancelled(slot.getSlotId())));

      val cancelledDay = new DayScheduleCancelled(scheduled.getDayId(), reason);

      return Either.right(cancelledBookings.appendAll(cancelledSlots).append(cancelledDay));
    }
    return null;
  }
}
