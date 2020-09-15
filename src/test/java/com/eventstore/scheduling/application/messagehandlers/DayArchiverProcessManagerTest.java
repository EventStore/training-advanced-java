package com.eventstore.scheduling.application.messagehandlers;

import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.readmodel.archivabledays.ArchivableDaysRepository;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.Archive;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.*;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.PatientId;
import com.eventstore.scheduling.infrastructure.eventstore.EsEventStoreClient;
import com.eventstore.scheduling.infrastructure.eventstore.EsEventSerde;
import com.eventstore.scheduling.infrastructure.inmemory.InMemoryColdStorage;
import com.eventstore.scheduling.infrastructure.mongodb.MongoArchivableDaysRepository;
import com.eventstore.scheduling.test.EventHandlerTest;
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

public class DayArchiverProcessManagerTest extends EventHandlerTest
    implements TestEventStoreConnection, TestMongoConnection {
  private final EventStoreClient<EventMetadata> eventStoreClient =
      new EsEventStoreClient<>(streamsClient, new EsEventSerde(), "test");
  private final InMemoryColdStorage coldStorage = new InMemoryColdStorage();
  private final ArchivableDaysRepository repository = new MongoArchivableDaysRepository(getMongo());

  @Override
  protected MessageHandler<EventMetadata> handler() {
    return new DayArchiverProcessManager(
        coldStorage, eventStoreClient, repository, commandStore, idGenerator, Period.ofDays(180));
  }

  @SneakyThrows
  @Test
  void shouldArchiveAllEventsAndTruncateAllExceptLastOne() {
    RandomIdGenerator idGenerator = new RandomIdGenerator();
    DoctorId doctorId = new DoctorId(idGenerator.nextUuid().toString());
    DayId dayId = new DayId(doctorId, today);
    val aggregateId = new DoctorDayId(dayId);
    val slotScheduled =
        new SlotScheduled(SlotId.create(this.idGenerator), dayId, tenAmToday, tenMinutes);
    val slotBooked = new SlotBooked(slotScheduled.getSlotId(), new PatientId("John Doe"));
    val archived = new DayScheduleArchived(dayId);

    val events = List.of(slotScheduled, slotBooked, archived);
    EventMetadata eventMetadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());

    eventStoreClient.createNewStream(aggregateId.toString(), events, eventMetadata);
    when(archived);
    then(
        eventStoreClient
            .readFromStream(aggregateId.toString(), None())
            .map(MessageEnvelope::getData),
        List.of(archived));
    then(coldStorage.events.map(MessageEnvelope::getData), events);
  }

  @Test
  void shouldSendArchiveCommandForAllSlotsCompleted180DaysAgo() {
    val aggregateId = new DoctorDayId(dayId);
    val date = LocalDate.now().minusDays(180);
    val dayScheduled = new DayScheduled(dayId, doctorId, date);
    val calendarDayStarted = new CalendarDayStarted(LocalDate.now());

    given(List.of(dayScheduled));
    when(calendarDayStarted);
    then(
        List.of(
            new CommandEnvelope(
                new Archive(),
                new CommandMetadata(
                    lastEventMetadata.getCorrelationId(),
                    CausationId.create(idGenerator),
                    aggregateId))));
  }
}
