package com.eventstore.scheduling.domain.writemodel.doctorday.state;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.*;
import io.vavr.collection.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.val;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class Scheduled extends State {
  private final @NonNull DayId dayId;
  private final @NonNull LocalDate date;
  private final @NonNull Slots slots;

  @Override
  public State apply(Event event) {
    if (event instanceof SlotScheduled) {
      val slotScheduled = (SlotScheduled) event;
      return new Scheduled(dayId, date, slots.add(slotScheduled));
    }
    if (event instanceof SlotBooked) {
      val slotBooked = (SlotBooked) event;
      return new Scheduled(dayId, date, slots.markAsBooked(slotBooked.getSlotId()));
    }
    if (event instanceof SlotBookingCancelled) {
      val slotBookingCancelled = (SlotBookingCancelled) event;
      return new Scheduled(dayId, date, slots.markAsAvailable(slotBookingCancelled.getSlotId()));
    }
    if (event instanceof SlotCancelled) {
      val slotCancelled = (SlotCancelled) event;
      return new Scheduled(dayId, date, slots.remove(slotCancelled.getSlotId()));
    }
    if (event instanceof DayScheduleCancelled) {
      return new Cancelled(dayId);
    }
    if (event instanceof DayScheduleArchived) {
      return new Archived();
    }
    throw new InvalidStateTransition(this, event);
  }

  public boolean hasAvaliableSlot(SlotId slotId) {
    return slots
        .getSlots()
        .filter((slot) -> !slot.getBooked())
        .exists((slot) -> slot.getSlotId().equals(slotId));
  }

  public boolean hasSlot(SlotId slotId) {
    return slots.getSlots().exists((slot) -> slot.getSlotId().equals(slotId));
  }

  public boolean hasBookedSlot(SlotId slotId) {
    return slots
        .getSlots()
        .filter(Slot::getBooked)
        .exists((slot) -> slot.getSlotId().equals(slotId));
  }

  public boolean doesNotOverlap(LocalTime startTime, Duration duration) {
    return !slots.getSlots().exists((slot) -> (slot.overlapsWith(startTime, duration)));
  }

  public List<Slot> getBookedSlots() {
    return slots.getSlots().filter(Slot::getBooked);
  }

  public List<Slot> getAllSlots() {
    return slots.getSlots();
  }
}
