package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.*;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.BookSlot;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.CancelDaySchedule;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.ScheduleDay;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.*;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.Scheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.Slots;
import com.eventstore.scheduling.infrastructure.eventstore.*;
import com.eventstore.scheduling.test.TestEventStoreConnection;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.eventstore.scheduling.test.TestFixtures.*;
import static io.vavr.API.None;
import static io.vavr.API.Some;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandHandlerTest implements TestEventStoreConnection {
  private final RandomIdGenerator idGenerator = new RandomIdGenerator();
  private final EventStoreClient<EventMetadata> eventStoreClient =
      new EsEventStoreClient<>(streamsClient, new EsEventSerde(), "test");
  private final AggregateStore aggregateStore = new EsAggregateStore(eventStoreClient);
  private final SnapshotStore snapshotStore =
      new EsSnapshotStore(new EsEventStoreClient<>(streamsClient, new EsSnapshotSerde(), "test"));

  @SneakyThrows
  @Test
  void shouldLoadAnAggregateAndHandleCommand() {
    val commandHandler =
        new CommandHandler<>(
            new DoctorDayLogic(idGenerator), aggregateStore, snapshotStore, None());
    val aggregateId = new DoctorDayId(new DayId(new DoctorId(randomString()), today));
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val bookSlot = new BookSlot(slotScheduled.getSlotId(), patientId);

    EventMetadata eventMetadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());
    CommandMetadata commandMetadata =
        new CommandMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), aggregateId);

    eventStoreClient.createNewStream(
        aggregateId.toString(), List.of(dayScheduled, slotScheduled), eventMetadata);

    commandHandler.handle(bookSlot, commandMetadata);

    MessageEnvelope<EventMetadata> lastEvent =
        eventStoreClient.readFromStream(aggregateId.toString(), None()).last();

    assertEquals(lastEvent.getData(), new SlotBooked(slotScheduled.getSlotId(), patientId));

    assertEquals(
        lastEvent.getMetadata(),
        new EventMetadata(
            commandMetadata.getCorrelationId(), commandMetadata.getCausationId(), None()));
  }

  @SneakyThrows
  @Test
  void shouldCreateSnapshotWhenCommittingChanges() {
    val commandHandler =
        new CommandHandler<>(
            new DoctorDayLogic(idGenerator), aggregateStore, snapshotStore, Some(1L));

    DoctorId doctorId = new DoctorId(randomString());
    val dayId = new DayId(doctorId, today);
    val aggregateId = new DoctorDayId(dayId);
    val scheduleDay = new ScheduleDay(doctorId, today, List.empty());

    CommandMetadata commandMetadata =
        new CommandMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), aggregateId);

    commandHandler.handle(scheduleDay, commandMetadata);

    Scheduled expected = new Scheduled(dayId, scheduleDay.getDate(), new Slots(List.empty()));
    assertEquals(expected, snapshotStore.read(aggregateId).get().getSnapshot());
  }

  @SneakyThrows
  @Test
  void shouldReadSnapshotIfPresentAndLoadRemainingEvents() {
    val commandHandler =
        new CommandHandler<>(
            new DoctorDayLogic(idGenerator), aggregateStore, snapshotStore, None());

    DoctorId doctorId = new DoctorId(randomString());
    val dayId = new DayId(doctorId, today);
    val aggregateId = new DoctorDayId(dayId);
    val dayScheduled = new DayScheduled(dayId, doctorId, today);
    val slotScheduled =
        new SlotScheduled(
            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);

    val aggregate = Aggregate.instance(doctorDayId, new DoctorDayLogic(idGenerator));
    val reconstituted = aggregate.reconstitute(List.of(dayScheduled));

    SnapshotMetadata snapshotMetadata =
        new SnapshotMetadata(
            CorrelationId.create(idGenerator),
            CausationId.create(idGenerator),
            reconstituted.getVersion());
    EventMetadata eventMetadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());
    CommandMetadata commandMetadata =
        new CommandMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), aggregateId);

    List<Event> eventsToWrite = List.of(dayScheduled, slotScheduled);

    eventStoreClient.createNewStream(aggregateId.toString(), eventsToWrite, eventMetadata);
    eventStoreClient.truncateStream(aggregateId.toString(), new Version(1L));
    snapshotStore.write(aggregateId, reconstituted.getState(), snapshotMetadata);

    val result = commandHandler.handle(new CancelDaySchedule("Not needed"), commandMetadata);

    List<Event> expected =
        List.of(
            slotScheduled,
            new SlotCancelled(slotScheduled.getSlotId()),
            new DayScheduleCancelled(dayId, "Not needed"));
    List<Object> actual =
        eventStoreClient
            .readFromStream(aggregateId.toString(), None())
            .map(MessageEnvelope::getData);

    assertTrue(result instanceof Either.Right<?, ?>);
    assertEquals(expected, actual);
  }
}
