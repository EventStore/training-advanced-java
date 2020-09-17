//package com.eventstore.scheduling.infrastructure.eventstore;
//
//import com.eventstore.scheduling.domain.service.IdGenerator;
//import com.eventstore.scheduling.domain.service.RandomIdGenerator;
//import com.eventstore.scheduling.domain.doctorday.command.ScheduleDay;
//import com.eventstore.scheduling.domain.doctorday.event.DayScheduled;
//import com.eventstore.scheduling.eventsourcing.Event;
//import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
//import com.eventstore.scheduling.eventsourcing.*;
//import com.eventstore.scheduling.test.TestEventStoreConnection;
//import io.vavr.collection.List;
//import lombok.val;
//import org.junit.jupiter.api.Test;
//
//import static com.eventstore.scheduling.test.TestFixtures.*;
//import static io.vavr.API.None;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class EsAggregateStoreTest implements TestEventStoreConnection {
//  private final EsEventStoreClient<EventMetadata> eventStore =
//      new EsEventStoreClient<>(streamsClient, new EsEventSerde(), "test");
//  private final AggregateStore aggregateStore = new EsAggregateStore(eventStore);
//  private final IdGenerator idGenerator = new RandomIdGenerator();
//
//  @Test
//  void reconstituteTheAggregate() {
//    val aggregateId = new DoctorDayId(new DayId(new DoctorId(randomString()), today));
//    val dayScheduled = new DayScheduled(dayId, doctorId, today);
//    val slotScheduled =
//        new SlotScheduled(
//            SlotId.create(idGenerator), dayScheduled.getDayId(), tenAmToday, tenMinutes);
//
//    EventMetadata eventMetadata =
//        new EventMetadata(
//            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());
//    List<Event> events = List.of(dayScheduled, slotScheduled);
//
//    eventStore.createNewStream(aggregateId.toString(), events, eventMetadata);
//    val aggregate = Aggregate.instance(aggregateId, new DoctorDayLogic(idGenerator));
//
//    assertEquals(aggregate.reconstitute(events), aggregateStore.reconsititute(aggregate));
//  }
//
//  @Test
//  void commitChanges() {
//    val eventStore = new EsEventStoreClient<>(streamsClient, new EsEventSerde(), "test");
//    val aggregateStore = new EsAggregateStore(eventStore);
//
//    val idGenerator = new RandomIdGenerator();
//
//    DoctorId doctorId = new DoctorId(randomString());
//    DayId dayId = new DayId(doctorId, today);
//    val aggregateId = new DoctorDayId(dayId);
//    val scheduleDay = new ScheduleDay(doctorId, today, List.empty());
//
//    EventMetadata eventMetadata =
//        new EventMetadata(
//            CorrelationId.create(idGenerator), CausationId.create(idGenerator), None());
//
//    val aggregate =
//        Aggregate.instance(aggregateId, new DoctorDayLogic(idGenerator)).handle(scheduleDay).get();
//    val committed = aggregateStore.commit(aggregate, eventMetadata);
//    val allEvents =
//        eventStore.readFromStream(aggregateId.toString(), None()).map(MessageEnvelope::getData);
//
//    assertEquals(aggregate.markAsCommitted(), committed);
//    assertEquals(List.of(new DayScheduled(dayId, doctorId, today)), allEvents);
//  }
//}
