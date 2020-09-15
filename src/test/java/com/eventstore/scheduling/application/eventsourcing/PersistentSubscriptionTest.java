package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.DayScheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.infrastructure.eventstore.EsCheckpointSerde;
import com.eventstore.scheduling.infrastructure.eventstore.EsCheckpointStore;
import com.eventstore.scheduling.infrastructure.eventstore.EsEventStoreClient;
import com.eventstore.scheduling.infrastructure.eventstore.EsEventSerde;
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

public class PersistentSubscriptionTest implements TestEventStoreConnection {
  final EventStoreClient<EventMetadata> eventStoreClient =
      new EsEventStoreClient<>(streamsClient, new EsEventSerde(), "test");
  final CheckpointStore checkpointStore =
      new EsCheckpointStore(new EsEventStoreClient<>(streamsClient, new EsCheckpointSerde(), "test"));
  private final IdGenerator idGenerator = new RandomIdGenerator();

  private final PersistentSubscriptionFactory<EventMetadata> factory =
      new PersistentSubscriptionFactory<>(eventStoreClient, checkpointStore);

  @SneakyThrows
  @Test
  void shouldSaveCheckpointAfterEveryMessage() {
    val streamId = "persistent-" + idGenerator.nextUuid().toString();
    val subscriptionId = new SubscriptionId("persistent-" + idGenerator.nextUuid().toString());
    val listReference = new AtomicReference<>(List.empty());

    val subscription =
        factory.subscribeToAStream(
            subscriptionId,
            streamId,
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
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes));

    val metadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());

    eventStoreClient.createNewStream(streamId, eventsToWrite, metadata);

    while (listReference.get().size() != eventsToWrite.size()) {
      Thread.sleep(100);
    }
    Thread.sleep(100);

    subscription.cancel(true);

    assertEquals(new Checkpoint(2L), checkpointStore.read(subscriptionId).get());
  }

  @SneakyThrows
  @Test
  void shouldStartFromSavedCheckpoint() {
    val streamId = "persistent-" + idGenerator.nextUuid().toString();
    val subscriptionId = new SubscriptionId("persistent-" + idGenerator.nextUuid().toString());
    val listReference = new AtomicReference<>(List.empty());

    checkpointStore.write(subscriptionId, new Checkpoint(1L));

    val subscription =
        factory.subscribeToAStream(
            subscriptionId,
            streamId,
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
            new SlotScheduled(SlotId.create(idGenerator), dayId, LocalDateTime.now(), tenMinutes));

    val metadata =
        new EventMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());

    eventStoreClient.createNewStream(streamId, eventsToWrite, metadata);

    while (listReference.get().size() != 1) {
      Thread.sleep(100);
    }
    Thread.sleep(100);

    subscription.cancel(true);

    Checkpoint actual = checkpointStore.read(subscriptionId).get();
    assertEquals(new Checkpoint(2L), actual);
  }
}
