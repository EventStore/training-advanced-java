package com.eventstore.scheduling;

import com.eventstore.scheduling.application.AvailableSlotsProjection;
import com.eventstore.scheduling.application.AvailableSlotsProjectionv2;
import com.eventstore.scheduling.domain.doctorday.PatientId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduleCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import com.eventstore.scheduling.infrastructure.mongodb.MongoAvailableSlotsRepository;
import com.eventstore.scheduling.infrastructure.mongodb.MongoAvailableSlotsRepositoryv2;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import com.eventstore.scheduling.test.HandlerTest;
import com.eventstore.scheduling.test.TestMongoConnection;
import io.vavr.collection.List;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.eventstore.scheduling.test.TestFixtures.*;

public class AvailableSlotsHandlerTestv2 extends HandlerTest implements TestMongoConnection {

  private final MongoAvailableSlotsRepositoryv2 repository = new MongoAvailableSlotsRepositoryv2(getMongo());
  private final EventHandler handler = new AvailableSlotsProjectionv2(repository);

  @Override
  protected EventHandler handler() {
    return handler;
  }

  @Test
  void shouldAddSlotToTheList() {
    val slotScheduled =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    given(List.of(slotScheduled));
    then(
        repository.getAvailableSlotsOn(today),
        List.of(
            new AvailableSlot(
                slotScheduled.dayId(),
                slotScheduled.slotId(),
                slotScheduled.startDateTime().toLocalDate(),
                slotScheduled.startDateTime().toLocalTime(),
                slotScheduled.duration().toString())));
  }

  @Test
  void shouldHideSlotFromTheListIfWasBooked() {
    val slotScheduled =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(dayId, slotScheduled.slotId(), new PatientId("John Doe"));

    given(List.of(slotScheduled, slotBooked));
    then(repository.getAvailableSlotsOn(today), List.empty());
  }

  @Test
  void shouldShowSlotIfBookingWasCancelled() {
    val slotScheduled =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(dayId, slotScheduled.slotId(), new PatientId("John Doe"));
    val slotBookingCancelled = new SlotBookingCancelled(dayId, slotScheduled.slotId(), "Don't need it");

    given(List.of(slotScheduled, slotBooked, slotBookingCancelled));
    then(
        repository.getAvailableSlotsOn(today),
        List.of(
            new AvailableSlot(
                slotScheduled.dayId(),
                slotScheduled.slotId(),
                slotScheduled.startDateTime().toLocalDate(),
                slotScheduled.startDateTime().toLocalTime(),
                slotScheduled.duration().toString())));
  }

  @Test
  void shouldDeleteSlotIfSlotWasCancelled() {
    val slotScheduled =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotCancelled = new SlotScheduleCancelled(dayId, slotScheduled.slotId());

    given(List.of(slotScheduled, slotCancelled));
    then(repository.getAvailableSlotsOn(today), List.empty());
  }
}
