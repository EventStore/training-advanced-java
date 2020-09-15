package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.domain.writemodel.AggregateId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayLogic;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.CancelSlotBooking;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.ScheduleDay;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduled;
import com.eventstore.scheduling.test.TestEventStoreConnection;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.eventstore.scheduling.test.TestFixtures.*;
import static io.vavr.API.None;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EsCommandStoreTest implements TestEventStoreConnection {
  private final IdGenerator idGenerator = new RandomIdGenerator();

  @Test
  void shouldSaveAsyncCommandsToTheStore() {
    val command = new CancelSlotBooking(slotId, "Dont't need it");
    val streamId = "test-commands";

    val commandMetadata =
        new CommandMetadata(
            CorrelationId.create(idGenerator),
            CausationId.create(idGenerator),
            new AggregateId(randomString(), "command-store-test"));

    val eventStore = new EsEventStoreClient<>(streamsClient, new EsCommandSerde(), "test");
    val commandStore = new EsCommandStore(streamId, eventStore);

    commandStore.send(command, commandMetadata);

    val expected = new CommandEnvelope(command, commandMetadata);
    val actual =
        eventStore
            .readLastFromStream(streamId)
            .map(envelope -> new CommandEnvelope(envelope.getData(), envelope.getMetadata()))
            .get();

    assertEquals(expected, actual);
  }

  @SneakyThrows
  @Test
  void shouldHandleAsyncCommands() {
    val doctorId = new DoctorId(randomString());
    val command = new ScheduleDay(doctorId, today, List.empty());
    val dayId = new DayId(doctorId, today);
    val streamId = "test-commands-" + randomString();

    val commandClient = new EsEventStoreClient<>(streamsClient, new EsCommandSerde(), "test");
    val commandStore = new EsCommandStore(streamId, commandClient);

    val eventClient = new EsEventStoreClient<>(streamsClient, new EsEventSerde(), "test");
    val aggregateStore = new EsAggregateStore(eventClient);

    val snapshotClient = new EsEventStoreClient<>(streamsClient, new EsSnapshotSerde(), "test");
    val snapshotStore = new EsSnapshotStore(snapshotClient);

    val checkpointClient = new EsEventStoreClient<>(streamsClient, new EsCheckpointSerde(), "test");
    val checkpointStore = new EsCheckpointStore(checkpointClient);

    val aggregateId = new DoctorDayId(dayId);
    val commandMetadata =
        new CommandMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), aggregateId);

    commandStore.send(command, commandMetadata);

    commandStore.subscribe(
        new CommandHandler<>(
            new DoctorDayLogic(idGenerator), aggregateStore, snapshotStore, None()),
        checkpointStore);

    while (eventClient.readLastFromStream(aggregateId.toString()).isEmpty()) {
      Thread.sleep(100);
    }

    val expected = new DayScheduled(dayId, doctorId, today);
    val actual =
        eventClient.readLastFromStream(aggregateId.toString()).map(MessageEnvelope::getData).get();
    assertEquals(expected, actual);
  }
}
