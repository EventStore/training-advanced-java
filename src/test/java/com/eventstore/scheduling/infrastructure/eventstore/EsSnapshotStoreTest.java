package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.scheduling.application.eventsourcing.CausationId;
import com.eventstore.scheduling.application.eventsourcing.CorrelationId;
import com.eventstore.scheduling.application.eventsourcing.SnapshotMetadata;
import com.eventstore.scheduling.application.eventsourcing.Version;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.Scheduled;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.Slot;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.Slots;
import com.eventstore.scheduling.test.TestEventStoreConnection;
import io.vavr.collection.List;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.eventstore.scheduling.test.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EsSnapshotStoreTest implements TestEventStoreConnection {
  @Test
  void shouldWriteAndReadSnapshots() {
    val idGenerator = new RandomIdGenerator();
    val snapshot =
        new Scheduled(dayId, today, new Slots(List.of(new Slot(slotId, tenAm, tenMinutes, true))));
    val snapshotStore =
        new EsSnapshotStore(new EsEventStoreClient<>(streamsClient, new EsSnapshotSerde(), "test"));

    SnapshotMetadata metadata =
        new SnapshotMetadata(
            CorrelationId.create(idGenerator), CausationId.create(idGenerator), new Version(1L));

    snapshotStore.write(doctorDayId, snapshot, metadata);

    val envelope = snapshotStore.read(doctorDayId).get();

    assertEquals(snapshot, envelope.getSnapshot());
    assertEquals(metadata, envelope.getMetadata());
  }
}
