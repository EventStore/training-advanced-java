package com.eventstore.scheduling;

import com.eventstore.scheduling.application.OverbookingProcessManager;
import com.eventstore.scheduling.domain.doctorday.PatientId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.command.CancelSlotBooking;
import com.eventstore.scheduling.domain.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository;
import com.eventstore.scheduling.eventsourcing.CommandEnvelope;
import com.eventstore.scheduling.eventsourcing.CommandStore;
import com.eventstore.scheduling.eventsourcing.EventStore;
import com.eventstore.scheduling.infrastructure.eventstore.EsCommandSerde;
import com.eventstore.scheduling.infrastructure.eventstore.EsCommandStore;
import com.eventstore.scheduling.infrastructure.eventstore.EsEventStore;
import com.eventstore.scheduling.infrastructure.mongodb.MongoBookedSlotsRepository;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import com.eventstore.scheduling.test.HandlerTest;
import com.eventstore.scheduling.test.TestEventStoreConnection;
import com.eventstore.scheduling.test.TestMongoConnection;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.eventstore.scheduling.test.TestFixtures.*;

public class OverbookingProcessManagerTest extends HandlerTest implements TestEventStoreConnection, TestMongoConnection {
    private final BookedSlotsRepository repository = new MongoBookedSlotsRepository(getMongo());
    private final EventStore eventStoreClient = new EsEventStore(client, "test");
    private final CommandStore commandStore = new EsCommandStore(eventStoreClient, client, null, "test", new EsCommandSerde());

    @Override
    protected EventHandler handler() {
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
        val slotBooked1 = new SlotBooked(dayId, slotScheduled1.slotId(), patientId);
        val slotBooked2 = new SlotBooked(dayId, slotScheduled2.slotId(), patientId);

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
        val slotBooked1 = new SlotBooked(dayId, slotScheduled1.slotId(), patientId);
        val slotBooked2 = new SlotBooked(dayId, slotScheduled2.slotId(), patientId);
        val slotBookingCancelled =
                new SlotBookingCancelled(dayId, slotScheduled2.slotId(), "No longer needed");

        given(List.of(slotScheduled1, slotScheduled2, slotBooked1, slotBooked2, slotBookingCancelled));
        then(repository.countByPatientAndMonth(patientId, today.getMonth()), 1);
    }

    @SneakyThrows
    @Test
    void shouldSendCommandToCancelSlotIfBookingLimitWasReached() {
        eventStoreClient.truncateStream("async-command-handler", eventStoreClient.getLastVersion("async-command-handler") + 1);

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
        val slotBooked1 = new SlotBooked(dayId, slotScheduled1.slotId(), patientId);
        val slotBooked2 = new SlotBooked(dayId, slotScheduled2.slotId(), patientId);
        val slotBooked3 = new SlotBooked(dayId, slotScheduled3.slotId(), patientId);
        val slotBooked4 = new SlotBooked(dayId, slotScheduled4.slotId(), patientId);

        given(
                List.of(
                        slotScheduled1,
                        slotScheduled2,
                        slotScheduled3,
                        slotScheduled4,
                        slotBooked1,
                        slotBooked2,
                        slotBooked3,
                        slotBooked4
                ));
        Thread.sleep(100);
        then(repository.countByPatientAndMonth(patientId, today.getMonth()), 4);
        then(
                eventStoreClient.loadCommands("async-command-handler").map(CommandEnvelope::command),
                List.of(new CancelSlotBooking(dayId, slotBooked4.slotId(), "Overbooking"))
        );
    }
}
