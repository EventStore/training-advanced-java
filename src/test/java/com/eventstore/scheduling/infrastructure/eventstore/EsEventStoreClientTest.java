package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.StreamNotFoundException;
import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.test.TestEventStoreConnection;
import io.vavr.collection.List;
import io.vavr.control.Option;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.eventstore.scheduling.test.TestFixtures.*;
import static io.vavr.API.None;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EsEventStoreClientTest implements TestEventStoreConnection {
  final EventStoreClient<EventMetadata> client =
      new EsEventStoreClient<>(streamsClient, new EsEventSerde(), "test");
  private final IdGenerator idGenerator = new RandomIdGenerator();

  @SneakyThrows
  @Test
  void shouldCreateNewStream() {
    val streamId = genStreamId();

    val eventsToWrite =
        List.of(
            new DayScheduled(dayId, doctorId, today),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes));

    val metadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());

    val version = client.createNewStream(streamId, eventsToWrite, metadata);
    val readEvents = client.readFromStream(streamId, None());

    assertEquals(new Version(1L), version);
    assertEquals(eventsToWrite, readEvents.map(MessageEnvelope::getData));
  }

  @SneakyThrows
  @Test
  void shouldAppendToAnExistingStream() {
    val streamId = genStreamId();

    val eventsToWrite =
        List.of(
            new DayScheduled(dayId, doctorId, today),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes));

    val metadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());

    val version = client.createNewStream(streamId, eventsToWrite, metadata);

    val eventsToWrite2 =
        List.of(
            new DayScheduled(dayId, doctorId, today),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes));
    val metadata2 =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());

    val version2 = client.appendToStream(streamId, eventsToWrite2, metadata2, version);

    val readEvents = client.readFromStream(streamId, None());

    assertEquals(new Version(1L), version);
    assertEquals(new Version(3L), version2);
    assertEquals(eventsToWrite.appendAll(eventsToWrite2), readEvents.map(MessageEnvelope::getData));
  }

  @SneakyThrows
  @Test
  void shouldTruncateEvents() {
    val streamId = genStreamId();

    val eventsToWrite =
        List.of(
            new DayScheduled(dayId, doctorId, today),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes));

    val metadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());

    client.createNewStream(streamId, eventsToWrite, metadata);
    client.truncateStream(streamId, new Version(3L));
    val readEvents = client.readFromStream(streamId, None());

    assertEquals(eventsToWrite.drop(3), readEvents.map(MessageEnvelope::getData));
  }

  @Test
  void shouldSoftDeleteStream() {
    val streamId = genStreamId();

    val eventsToWrite =
        List.of(
            new DayScheduled(dayId, doctorId, today),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes));

    val metadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());

    val version = client.createNewStream(streamId, eventsToWrite, metadata);
    client.deleteStream(streamId, version);

    assertThrows(StreamNotFoundException.class, () -> client.readFromStream(streamId, None()));
  }

  @SneakyThrows
  @Test
  void shouldSubscribeToAStream() {
    val streamId = genStreamId();
    val listReference = new AtomicReference<>(List.empty());

    val subscription =
        client.subscribeToAStream(
            streamId,
            None(),
            new MessageHandler<EventMetadata>() {
              @Override
              public void handle(
                  Object message,
                  EventMetadata metadata,
                  UUID messageId,
                  Instant occurredAt,
                  Version position,
                  Option<Version> streamPosition) {
                listReference.set(listReference.get().append(message));
              }
            });

    val eventsToWrite =
        List.of(
            new DayScheduled(dayId, doctorId, today),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes),
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes));

    val metadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());

    client.createNewStream(streamId, eventsToWrite, metadata);

    while (listReference.get().size() != eventsToWrite.size()) {
      Thread.sleep(100);
    }

    subscription.cancel(true);

    assertEquals(listReference.get(), eventsToWrite);
  }

  private String genStreamId() {
    return "aggregate-" + idGenerator.nextUuid().toString();
  }
}
