package com.eventstore.scheduling;

import com.eventstore.scheduling.domain.doctorday.Day;
import com.eventstore.scheduling.domain.doctorday.PatientId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.command.*;
import com.eventstore.scheduling.domain.doctorday.error.*;
import com.eventstore.scheduling.domain.doctorday.event.*;
import com.eventstore.scheduling.domain.service.DayRepository;
import com.eventstore.scheduling.domain.service.Handlers;
import com.eventstore.scheduling.eventsourcing.AggregateRoot;
import com.eventstore.scheduling.eventsourcing.AggregateStore;
import com.eventstore.scheduling.infrastructure.eventstore.EsDayRepository;
import com.eventstore.scheduling.test.AggregateTest;
import io.vavr.collection.List;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.stream.IntStream;

import static com.eventstore.scheduling.test.TestFixtures.*;

public class DayTests extends AggregateTest<Day, DayRepository> {

  public DayTests() {
    registerHandlers(new Handlers(repository, idGenerator));
  }

  @Test
  void shouldBeScheduled() {
    List<ScheduleSlot> slots =
        List.ofAll(IntStream.range(0, 30).toArray())
            .map((i) -> new ScheduleSlot(dayId, tenAm.plusMinutes(i * 10), tenMinutes));
    val scheduleDay = new ScheduleDay(doctorId, today, slots);


    val dayScheduled = new DayScheduled(dayId, scheduleDay.getDoctorId(), scheduleDay.getDate());
    List<Object> slotsScheduled =
        slots.map(
            (slot) ->
                new SlotScheduled(
                    SlotId.create(idGenerator),
                    dayScheduled.getDayId(),
                    LocalDateTime.of(today, slot.getStartTime()),
                    slot.getDuration()));

    given();
    when(scheduleDay);
    then(slotsScheduled.prepend(dayScheduled));
  }

  @Test
  void shouldNotBeScheduledTwice() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);

    List<ScheduleSlot> slots =
        List.ofAll(IntStream.range(0, 30).toArray())
            .map((i) -> new ScheduleSlot(dayId, tenAm.plusMinutes(i * 10), tenMinutes));
    val scheduleDay = new ScheduleDay(doctorId, today, slots);

    given(dayScheduled);
    when(scheduleDay);
    then(DayAlreadyScheduled.class);
  }

  @Test
  void shouldAllowToBookASlot() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val bookSlot = new BookSlot(dayId, slotScheduled.getSlotId(), new PatientId("John Doe"));

    given(dayScheduled, slotScheduled);
    when(bookSlot);
    then(List.of(new SlotBooked(dayId, slotScheduled.getSlotId(), bookSlot.getPatientId())));
  }

  @Test
  void shouldNotAllowToBookASlotTwice() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(dayId, slotScheduled.getSlotId(), new PatientId("John Doe"));

    val bookSlot = new BookSlot(dayId, slotScheduled.getSlotId(), new PatientId("John Doe"));

    given(dayScheduled, slotScheduled, slotBooked);
    when(bookSlot);
    then(SlotAlreadyBooked.class);
  }

  @Test
  void shouldNotAllowToBookASlotIfDayWasNotScheduled() {
    val bookSlot = new BookSlot(dayId, SlotId.create(idGenerator), new PatientId("John Doe"));

    given();
    when(bookSlot);
    then(DayNotScheduled.class);
  }

  @Test
  void shouldNotAllowToBookAnUnscheduledSlot() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val bookSlot = new BookSlot(dayId, SlotId.create(idGenerator), new PatientId("John Doe"));

    given((dayScheduled));
    when(bookSlot);
    then(SlotNotScheduled.class);
  }

  @Test
  void allowToCancelBooking() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(dayId, slotScheduled.getSlotId(), new PatientId("John Doe"));

    val cancel = new CancelSlotBooking(dayId, slotScheduled.getSlotId(), randomString());

    given(dayScheduled, slotScheduled, slotBooked);
    when(cancel);
    then(List.of(new SlotBookingCancelled(dayId, slotScheduled.getSlotId(), cancel.getReason())));
  }

  @Test
  void notAllowToCancelUnbookedSlot() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val cancel = new CancelSlotBooking(dayId, slotScheduled.getSlotId(), randomString());

    given(dayScheduled, slotScheduled);
    when(cancel);
    then(SlotNotBooked.class);
  }

  @Test
  void allowToScheduleAnExtraSlot() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);

    val scheduleSlot = new ScheduleSlot(dayId, tenAm, tenMinutes);

    List<Object> events = List.of(
            new SlotScheduled(
                    SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes));

    given(dayScheduled);
    when(scheduleSlot);
    then(events);
  }

  @Test
  void denyToScheduleOverlappingSlots() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val scheduleSlot = new ScheduleSlot(dayId, tenAm, tenMinutes);

    given(dayScheduled, slotScheduled);
    when(scheduleSlot);
    then(SlotOverlapped.class);
  }

  @Test
  void allowToScheduleAdjacentSlots() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(randomIdGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val scheduleSlot = new ScheduleSlot(dayId, tenAm.plusMinutes(10), tenMinutes);
    List<Object> events = List.of(
            new SlotScheduled(
                    SlotId.create(idGenerator),
                    dayScheduled.getDayId(),
                    tenAmToday.plusMinutes(10),
                    tenMinutes));

    given(dayScheduled, slotScheduled);
    when(scheduleSlot);
    then(events);
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
    val slotBooked = new SlotBooked(dayId, slotScheduled1.getSlotId(), new PatientId("John Doe"));

    String reason = "doctor cancelled the day";
    val cancelDaySchedule = new CancelDaySchedule(dayId);

    given(dayScheduled, slotScheduled1, slotScheduled2, slotBooked);
    when(cancelDaySchedule);
    then(
        List.of(
            new SlotBookingCancelled(dayId, slotScheduled1.getSlotId(), reason),
            new SlotScheduleCancelled(dayId, slotScheduled1.getSlotId()),
            new SlotScheduleCancelled(dayId, slotScheduled2.getSlotId()),
            new DayScheduleCancelled(dayScheduled.getDayId(), reason)));
  }

  @Test
  void archiveScheduledDay() {
    given((new DayScheduled(dayId, doctorId, today)));
    when(new ArchiveDaySchedule(dayId));
    then(List.of(new DayScheduleArchived(dayId)));
  }

  @Test
  void archiveCancelledDay() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val dayScheduleCancelled = new DayScheduleCancelled(dayScheduled.getDayId(), "Called in sick");

    given(dayScheduled, dayScheduleCancelled);
    when(new ArchiveDaySchedule(dayId));
    then(List.of(new DayScheduleArchived(dayId)));
  }

  @Test
  void rejectCommandsAfterScheduleWasArchived() {
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val dayScheduleArchived = new DayScheduleArchived(dayId);

    given(dayScheduled, dayScheduleArchived);
    when(new ScheduleDay(doctorId, today, List.empty()));
    then(DayScheduleAlreadyArchived.class);
  }

  @Override
  protected AggregateRoot aggregateInstance() {
    return new Day();
  }

  @Override
  protected DayRepository repositoryInstance(AggregateStore aggregateStore) {
    return new EsDayRepository(aggregateStore);
  }
}
