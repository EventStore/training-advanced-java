package com.eventstore.scheduling.application.messagehandlers;

import com.eventstore.scheduling.application.eventsourcing.EventMetadata;
import com.eventstore.scheduling.application.eventsourcing.MessageHandler;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlotsRepository;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.PatientId;
import com.eventstore.scheduling.infrastructure.mongodb.MongoAvailableSlotsRepository;
import com.eventstore.scheduling.test.EventHandlerTest;
import com.eventstore.scheduling.test.TestMongoConnection;
import io.vavr.collection.List;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.eventstore.scheduling.test.TestFixtures.*;

public class AvailableSlotsProjectorTest extends EventHandlerTest implements TestMongoConnection {

  private final AvailableSlotsRepository repository = new MongoAvailableSlotsRepository(getMongo());
  private final MessageHandler<EventMetadata> handler = new AvailableSlotsProjector(repository);

  @Override
  protected MessageHandler<EventMetadata> handler() {
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
    val slotBooked = new SlotBooked(slotScheduled.getSlotId(), new PatientId("John Doe"));

    given(List.of(slotScheduled, slotBooked));
    then(repository.getAvailableSlotsOn(today), List.empty());
  }

  @Test
  void shouldShowSlotIfBookingWasCancelled() {
    val slotScheduled =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(slotScheduled.getSlotId(), new PatientId("John Doe"));
    val slotBookingCancelled = new SlotBookingCancelled(slotScheduled.getSlotId(), "Don't need it");

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
    val slotCancelled = new SlotCancelled(slotScheduled.getSlotId());

    given(List.of(slotScheduled, slotCancelled));
    then(repository.getAvailableSlotsOn(today), List.empty());
  }
}
