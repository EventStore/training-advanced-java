package com.eventstore.scheduling;

import com.eventstore.scheduling.application.AvailableSlotsProjection;
import com.eventstore.scheduling.domain.doctorday.PatientId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduleCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import com.eventstore.scheduling.infrastructure.mongodb.MongoAvailableSlotsRepository;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import com.eventstore.scheduling.test.HandlerTest;
import com.eventstore.scheduling.test.TestMongoConnection;
import io.vavr.collection.List;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.eventstore.scheduling.test.TestFixtures.*;

public class AvailableSlotsHandlerTest extends HandlerTest implements TestMongoConnection {

  private final MongoAvailableSlotsRepository repository = new MongoAvailableSlotsRepository(getMongo());
  private final EventHandler handler = new AvailableSlotsProjection(repository);

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
                slotScheduled.getDayId(),
                slotScheduled.getSlotId(),
                slotScheduled.getStartDateTime().toLocalDate(),
                slotScheduled.getStartDateTime().toLocalTime(),
                slotScheduled.getDuration().toString())));
  }

  @Test
  void shouldHideSlotFromTheListIfWasBooked() {
    val slotScheduled =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(dayId, slotScheduled.getSlotId(), new PatientId("John Doe"));

    given(List.of(slotScheduled, slotBooked));
    then(repository.getAvailableSlotsOn(today), List.empty());
  }

  @Test
  void shouldShowSlotIfBookingWasCancelled() {
    val slotScheduled =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(dayId, slotScheduled.getSlotId(), new PatientId("John Doe"));
    val slotBookingCancelled = new SlotBookingCancelled(dayId, slotScheduled.getSlotId(), "Don't need it");

    given(List.of(slotScheduled, slotBooked, slotBookingCancelled));
    then(
        repository.getAvailableSlotsOn(today),
        List.of(
            new AvailableSlot(
                slotScheduled.getDayId(),
                slotScheduled.getSlotId(),
                slotScheduled.getStartDateTime().toLocalDate(),
                slotScheduled.getStartDateTime().toLocalTime(),
                slotScheduled.getDuration().toString())));
  }

  @Test
  void shouldDeleteSlotIfSlotWasCancelled() {
    val slotScheduled =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotCancelled = new SlotScheduleCancelled(dayId, slotScheduled.getSlotId());

    given(List.of(slotScheduled, slotCancelled));
    then(repository.getAvailableSlotsOn(today), List.empty());
  }
}
