//package com.eventstore.scheduling.infrastructure.eventstore;
//
//import com.eventstore.dbclient.Position;
//import com.eventstore.dbclient.RecordedEvent;
//import com.eventstore.dbclient.ResolvedEvent;
//import com.eventstore.dbclient.StreamRevision;
//import com.eventstore.scheduling.eventsourcing.CausationId;
//import com.eventstore.scheduling.eventsourcing.CorrelationId;
//import com.eventstore.scheduling.eventsourcing.EventMetadata;
//import com.eventstore.scheduling.eventsourcing.MessageEnvelope;
//import com.eventstore.scheduling.domain.service.IdGenerator;
//import com.eventstore.scheduling.domain.service.RandomIdGenerator;
//import io.vavr.Tuple2;
//import io.vavr.collection.HashMap;
//import lombok.val;
//import org.junit.jupiter.api.Test;
//
//import static com.eventstore.scheduling.test.TestFixtures.*;
//import static io.vavr.API.Some;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//public class EsEventSerdeTest {
//  final EsEventSerde serde = new EsEventSerde();
//  private final IdGenerator idGenerator = new RandomIdGenerator();
//  private final EventMetadata eventMetadata =
//      new EventMetadata(
//          CorrelationId.create(idGenerator), CausationId.create(idGenerator), Some("yes"));
//
//  @Test
//  void shouldRoundTripAllEventTypes() {
//    roundTrip(new DayScheduled(dayId, doctorId, today), "doctorday-day-scheduled");
//    roundTrip(new DayScheduleArchived(dayId), "doctorday-day-schedule-archived");
//    roundTrip(new SlotScheduled(slotId, dayId, tenAmToday, tenMinutes), "doctorday-slot-scheduled");
//    roundTrip(new SlotBooked(slotId, patientId), "doctorday-slot-booked");
//    roundTrip(new SlotBookingCancelled(slotId, randomString()), "doctorday-slot-booking-cancelled");
//    roundTrip(new SlotScheduleCancelled(slotId), "doctorday-slot-cancelled");
//    roundTrip(
//        new DayScheduleCancelled(dayId, "Called in sick"), "doctorday-day-schedule-cancelled");
//    roundTrip(new CalendarDayStarted(today), "doctorday-calendar-day-started");
//  }
//
//  private void roundTrip(Object expectedEvent, String type) {
//    val proposedEvent = serde.serialize(expectedEvent, eventMetadata).get();
//    val resolvedEvent =
//        new ResolvedEvent(
//            new RecordedEvent(
//                randomString(),
//                new StreamRevision(0L),
//                idGenerator.nextUuid(),
//                new Position(0L, 0L),
//                HashMap.ofEntries(
//                        new Tuple2("type", type),
//                        new Tuple2("", "application/json"),
//                        new Tuple2("created", "0"))
//                    .toJavaMap(),
//                proposedEvent.getEventData(),
//                proposedEvent.getUserMetadata()),
//            null);
//    MessageEnvelope<EventMetadata> deserialized = serde.deserialize(resolvedEvent).get();
//
//    assertEquals(eventMetadata, deserialized.getMetadata());
//    assertEquals(expectedEvent, deserialized.getData());
//  }
//}
