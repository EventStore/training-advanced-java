package com.eventstore.scheduling.domain.writemodel;

import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayLogic;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.*;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.*;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.*;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.PatientId;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.State;
import com.eventstore.scheduling.test.AggregateTest;
import io.vavr.collection.List;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static com.eventstore.scheduling.test.TestFixtures.*;

public class DoctorDayAggregateTest extends AggregateTest<Command, Event, Error, State> {

  @Override
  protected AggregateLogic<Command, Event, Error, State> aggregateLogic() {
    return new DoctorDayLogic(idGenerator);
  }

  @Test
  void shouldBeScheduled() {
    val slots =
        List.ofAll(IntStream.range(0, 30).toArray())
            .map((i) -> new ScheduleSlot(tenAm.plusMinutes(i * 10), tenMinutes));
    val scheduleDay = new ScheduleDay(doctorId, today, slots);

    when(scheduleDay);

    val dayScheduled = new DayScheduled(dayId, scheduleDay.getDoctorId(), scheduleDay.getDate());
    List<Event> slotsScheduled =
        slots.map(
            (slot) ->
                new SlotScheduled(
                    SlotId.create(idGenerator),
                    dayScheduled.getDayId(),
                    LocalDateTime.of(today, slot.getStartTime()),
                    slot.getDuration()));

    then(slotsScheduled.prepend(dayScheduled));
  }

  @Test
  void shouldNotBeScheduledTwice() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);

    val slots =
        List.ofAll(IntStream.range(0, 30).toArray())
            .map((i) -> new ScheduleSlot(tenAm.plusMinutes(i * 10), tenMinutes));
    val scheduleDay = new ScheduleDay(doctorId, today, slots);

    given(List.of(dayScheduled));
    when(scheduleDay);
    then(new DayAlreadyScheduled());
  }

  @Test
  void shouldAllowToBookASlot() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val bookSlot = new BookSlot(slotScheduled.getSlotId(), new PatientId("John Doe"));

    given(List.of(dayScheduled, slotScheduled));
    when(bookSlot);
    then(List.of(new SlotBooked(slotScheduled.getSlotId(), bookSlot.getPatientId())));
  }

  @Test
  void shouldNotAllowToBookASlotTwice() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(slotScheduled.getSlotId(), new PatientId("John Doe"));

    val bookSlot = new BookSlot(slotScheduled.getSlotId(), new PatientId("John Doe"));

    given(List.of(dayScheduled, slotScheduled, slotBooked));
    when(bookSlot);
    then(new SlotAlreadyBooked());
  }

  @Test
  void shouldNotAllowToBookASlotIfDayWasNotScheduled() {
    val bookSlot = new BookSlot(SlotId.create(idGenerator), new PatientId("John Doe"));

    given(List.empty());
    when(bookSlot);
    then(new DayNotScheduled());
  }

  @Test
  void shouldNotAllowToBookAnUnscheduledSlot() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val bookSlot = new BookSlot(SlotId.create(idGenerator), new PatientId("John Doe"));

    given(List.of(dayScheduled));
    when(bookSlot);
    then(new SlotNotScheduled());
  }

  @Test
  void allowToCancelBooking() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(slotScheduled.getSlotId(), new PatientId("John Doe"));

    val cancel = new CancelSlotBooking(slotScheduled.getSlotId(), randomString());

    given(List.of(dayScheduled, slotScheduled, slotBooked));
    when(cancel);
    then(List.of(new SlotBookingCancelled(slotScheduled.getSlotId(), cancel.getReason())));
  }

  @Test
  void notAllowToCancelUnbookedSlot() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val cancel = new CancelSlotBooking(slotScheduled.getSlotId(), randomString());

    given(List.of(dayScheduled, slotScheduled));
    when(cancel);
    then(new SlotNotBooked());
  }

  @Test
  void allowToScheduleAnExtraSlot() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);

    val scheduleSlot = new ScheduleSlot(tenAm, tenMinutes);

    given(List.of(dayScheduled));
    when(scheduleSlot);
    then(
        List.of(
            new SlotScheduled(
                SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes)));
  }

  @Test
  void denyToScheduleOverlappingSlots() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val scheduleSlot = new ScheduleSlot(tenAm, tenMinutes);

    given(List.of(dayScheduled, slotScheduled));
    when(scheduleSlot);
    then(new SlotOverlapped());
  }

  @Test
  void allowToScheduleAdjacentSlots() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val scheduleSlot = new ScheduleSlot(tenAm.plusMinutes(10), tenMinutes);

    given(List.of(dayScheduled, slotScheduled));
    when(scheduleSlot);
    then(
        List.of(
            new SlotScheduled(
                SlotId.create(idGenerator),
                dayScheduled.getDayId(),
                tenAmToday.plusMinutes(10),
                tenMinutes)));
  }

  @Test
  void cancelBookedSlotsWhenTheDayIsCancelled() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled1 =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);
    val slotScheduled2 =
        new SlotScheduled(
            SlotId.create(idGenerator),
            dayScheduled.getDayId(),
            tenAmToday.plusMinutes(10),
            tenMinutes);
    val slotBooked = new SlotBooked(slotScheduled1.getSlotId(), new PatientId("John Doe"));

    String reason = "Called in sick";
    val cancelDaySchedule = new CancelDaySchedule(reason);

    given(List.of(dayScheduled, slotScheduled1, slotScheduled2, slotBooked));
    when(cancelDaySchedule);
    then(
        List.of(
            new SlotBookingCancelled(slotScheduled1.getSlotId(), reason),
            new SlotCancelled(slotScheduled1.getSlotId()),
            new SlotCancelled(slotScheduled2.getSlotId()),
            new DayScheduleCancelled(dayScheduled.getDayId(), reason)));
  }

  @Test
  void archiveScheduledDay() {
    given(List.of(new DayScheduled(dayId, doctorId, today)));
    when(new Archive());
    then(List.of(new DayScheduleArchived(dayId)));
  }

  @Test
  void archiveCancelledDay() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val dayScheduleCancelled = new DayScheduleCancelled(dayScheduled.getDayId(), "Called in sick");

    given(List.of(dayScheduled, dayScheduleCancelled));
    when(new Archive());
    then(List.of(new DayScheduleArchived(dayId)));
  }

  @Test
  void rejectCommandsAfterScheduleWasArchived() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val dayScheduleArchived = new DayScheduleArchived(dayId);

    given(List.of(dayScheduled, dayScheduleArchived));
    when(new ScheduleDay(doctorId, today, List.empty()));
    then(new DayScheduleAlreadyArchived());
  }
}
