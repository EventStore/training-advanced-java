package com.eventstore.scheduling.application.messagehandlers;

import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.CancelSlotBooking;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.PatientId;
import com.eventstore.scheduling.infrastructure.mongodb.MongoBookedSlotsRepository;
import com.eventstore.scheduling.test.EventHandlerTest;
import com.eventstore.scheduling.test.TestMongoConnection;
import io.vavr.collection.List;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.eventstore.scheduling.test.TestFixtures.*;

public class OverbookingProcessManagerTest extends EventHandlerTest implements TestMongoConnection {
  private final BookedSlotsRepository repository = new MongoBookedSlotsRepository(getMongo());

  @Override
  protected MessageHandler<EventMetadata> handler() {
    int bookingLimitPerPatient = 3;
    return new OverbookingProcessManager(
        repository, commandStore, bookingLimitPerPatient, idGenerator);
  }

  @Test
  void shouldIncrementTheVisitCounterWhenSlotIsBooked() {
    val patientId = new PatientId("John Doe");
    val slotScheduled1 =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotScheduled2 =
        new SlotScheduled(
            SlotId.create(idGenerator), dayId, tenAmToday.plusMinutes(10), tenMinutes);
    val slotBooked1 = new SlotBooked(slotScheduled1.getSlotId(), patientId);
    val slotBooked2 = new SlotBooked(slotScheduled2.getSlotId(), patientId);

    given(List.of(slotScheduled1, slotScheduled2, slotBooked1, slotBooked2));
    then(repository.countByPatientAndMonth(patientId, today.getMonth()), 2);
  }

  @Test
  void shouldDecrementTheVisitCounterWhenSlotBookingIsCancelled() {
    val patientId = new PatientId("John Doe");
    val slotScheduled1 =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotScheduled2 =
        new SlotScheduled(
            SlotId.create(idGenerator), dayId, tenAmToday.plusMinutes(10), tenMinutes);
    val slotBooked1 = new SlotBooked(slotScheduled1.getSlotId(), patientId);
    val slotBooked2 = new SlotBooked(slotScheduled2.getSlotId(), patientId);
    val slotBookingCancelled =
        new SlotBookingCancelled(slotScheduled2.getSlotId(), "No longer needed");

    given(List.of(slotScheduled1, slotScheduled2, slotBooked1, slotBooked2, slotBookingCancelled));
    then(repository.countByPatientAndMonth(patientId, today.getMonth()), 1);
  }

  @Test
  void shouldCommandToCancelSlotIfBookingLimitWasReached() {
    val patientId = new PatientId("John Doe");
    val slotScheduled1 =
        new SlotScheduled(SlotId.create(idGenerator), dayId, tenAmToday, tenMinutes);
    val slotScheduled2 =
        new SlotScheduled(
            SlotId.create(idGenerator), dayId, tenAmToday.plusMinutes(10), tenMinutes);
    val slotScheduled3 =
        new SlotScheduled(
            SlotId.create(idGenerator), dayId, tenAmToday.plusMinutes(20), tenMinutes);
    val slotScheduled4 =
        new SlotScheduled(
            SlotId.create(idGenerator), dayId, tenAmToday.plusMinutes(30), tenMinutes);
    val slotBooked1 = new SlotBooked(slotScheduled1.getSlotId(), patientId);
    val slotBooked2 = new SlotBooked(slotScheduled2.getSlotId(), patientId);
    val slotBooked3 = new SlotBooked(slotScheduled3.getSlotId(), patientId);
    val slotBooked4 = new SlotBooked(slotScheduled4.getSlotId(), patientId);

    given(
        List.of(
            slotScheduled1,
            slotScheduled2,
            slotScheduled3,
            slotScheduled4,
            slotBooked1,
            slotBooked2,
            slotBooked3));
    when(slotBooked4);
    then(repository.countByPatientAndMonth(patientId, today.getMonth()), 4);
    then(
        List.of(
            new CommandEnvelope(
                new CancelSlotBooking(slotBooked4.getSlotId(), "Overbooking"),
                new CommandMetadata(
                    lastEventMetadata.getCorrelationId(),
                    CausationId.create(idGenerator),
                    new DoctorDayId(dayId)))));
  }
}
