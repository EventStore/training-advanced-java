package com.eventstore.scheduling;

import com.eventstore.scheduling.application.DayArchiverProcessManager;
import com.eventstore.scheduling.domain.doctorday.*;
import com.eventstore.scheduling.domain.doctorday.command.ArchiveDaySchedule;
import com.eventstore.scheduling.domain.doctorday.event.*;
import com.eventstore.scheduling.domain.readmodel.archivabledays.ArchivableDaysRepository;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.eventsourcing.*;
import com.eventstore.scheduling.infrastructure.eventstore.EsCommandSerde;
import com.eventstore.scheduling.infrastructure.eventstore.EsCommandStore;
import com.eventstore.scheduling.infrastructure.eventstore.EsEventStore;
import com.eventstore.scheduling.infrastructure.inmemory.InMemoryColdStorage;
import com.eventstore.scheduling.infrastructure.mongodb.MongoArchivableDaysRepository;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import com.eventstore.scheduling.test.HandlerTest;
import com.eventstore.scheduling.test.TestEventStoreConnection;
import com.eventstore.scheduling.test.TestMongoConnection;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static com.eventstore.scheduling.test.TestFixtures.*;
import static io.vavr.API.None;

public class DayArchiverProcessManagerTest extends HandlerTest
        implements TestEventStoreConnection, TestMongoConnection {
    private final EventStore eventStoreClient = new EsEventStore(client, "test");
    private final InMemoryColdStorage coldStorage = new InMemoryColdStorage();
    private final ArchivableDaysRepository repository = new MongoArchivableDaysRepository(getMongo());
    private final CommandStore commandStore = new EsCommandStore(eventStoreClient, client, null, "test", new EsCommandSerde());

    @Override
    protected EventHandler handler() {
        return new DayArchiverProcessManager(
                coldStorage, eventStoreClient, repository, commandStore, idGenerator, Period.ofDays(180));
    }

    @SneakyThrows
    @Test
    void shouldArchiveAllEventsAndTruncateAllExceptLastOne() {
        val idGenerator = new RandomIdGenerator();
        val doctorId = new DoctorId(idGenerator.nextUuid().toString());
        val dayId = new DayId(doctorId, today);
        val aggregateId = new DoctorDayId(dayId);
        val slotScheduled =
                new SlotScheduled(SlotId.create(this.idGenerator), dayId, tenAmToday, tenMinutes);
        val slotBooked = new SlotBooked(dayId, slotScheduled.slotId(), new PatientId("John Doe"));
        val archived = new DayScheduleArchived(dayId);

        List<Object> events = List.of(slotScheduled, slotBooked, archived);
        val metadata = new CommandMetadata(CorrelationId.create(idGenerator), CausationId.create(idGenerator));

        eventStoreClient.appendEvents(aggregateId.toString(), metadata, events);
        given(List.of(archived));
        then(eventStoreClient.loadEvents(aggregateId.toString(), None()), List.of(archived));
        then(coldStorage.events, events);
    }

    @SneakyThrows
    @Test
    void shouldSendArchiveCommandForAllSlotsCompleted180DaysAgo() {
        eventStoreClient.truncateStream("async-command-handler", eventStoreClient.getLastVersion("async-command-handler") + 1);
        val date = LocalDate.now().minusDays(180);
        val dayScheduled = new DayScheduled(dayId, doctorId, date);
        val calendarDayStarted = new CalendarDayStarted(LocalDate.now());

        given(List.of(dayScheduled, calendarDayStarted));
        Thread.sleep(100);
        then(eventStoreClient.loadCommands("async-command-handler").map(CommandEnvelope::command),                List.of(new ArchiveDaySchedule(dayId)));
    }
}
