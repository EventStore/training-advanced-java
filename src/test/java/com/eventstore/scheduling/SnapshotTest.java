package com.eventstore.scheduling;

import com.eventstore.scheduling.domain.doctorday.Day;
import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DaySnapshot;
import com.eventstore.scheduling.domain.doctorday.command.ScheduleSlot;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.eventsourcing.*;
import com.eventstore.scheduling.infrastructure.eventstore.EsAggregateStore;
import com.eventstore.scheduling.infrastructure.eventstore.EsEventStore;
import com.eventstore.scheduling.test.TestEventStoreConnection;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static com.eventstore.scheduling.test.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SnapshotTest implements TestEventStoreConnection {
    private final EventStore eventStoreClient = new EsEventStore(client, "test");
    EsAggregateStore esAggregateStore = new EsAggregateStore(eventStoreClient, 5);

    @SneakyThrows
    @Test
    void shouldWriteSnapshotIfThresholdWasReached() {
        val idGenerator = new RandomIdGenerator();
        val aggregate = new Day();
        List<ScheduleSlot> slots =
                List.ofAll(IntStream.range(0, 5).toArray())
                        .map((i) -> new ScheduleSlot(dayId, tenAm.plusMinutes(i * 10), tenMinutes));
        aggregate.schedule(doctorId, today, slots, idGenerator);

        val metadata = new CommandMetadata(CorrelationId.create(idGenerator), CausationId.create(idGenerator));

        esAggregateStore.save(aggregate, metadata);

        val snapshot = new DaySnapshot(
                aggregate.slots.getSlots(), aggregate.isArchived, aggregate.isCancelled, aggregate.isScheduled, dayId, today
        );

        Thread.sleep(200);
        assertEquals(snapshot, eventStoreClient.loadSnapshot("doctorday-%s".formatted(dayId.value())).snapshot());
    }
    @SneakyThrows
    @Test
    void shouldReadSnapshotWhenLoading() {
        val anotherDayId = new DayId(doctorId, tomorrow);
        val idGenerator = new RandomIdGenerator();
        val aggregate = new Day();
        List<ScheduleSlot> slots =
                List.ofAll(IntStream.range(0, 5).toArray())
                        .map((i) -> new ScheduleSlot(anotherDayId, tenAm.plusMinutes(i * 10), tenMinutes));
        aggregate.schedule(doctorId, tomorrow, slots, idGenerator);

        val metadata = new CommandMetadata(CorrelationId.create(idGenerator), CausationId.create(idGenerator));

        esAggregateStore.save(aggregate, metadata);

        Thread.sleep(200);
        eventStoreClient.truncateStream("doctorday-%s".formatted(anotherDayId.value()), eventStoreClient.getLastVersion("doctorday-%s".formatted(anotherDayId.value())) + 1);
        val reloadedAggregate = esAggregateStore.load(new Day(), anotherDayId.value());

        assertEquals(reloadedAggregate.getVersion(), 5L);
    }
}
