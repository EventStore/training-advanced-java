package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DoctorId;
import com.eventstore.scheduling.domain.doctorday.PatientId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.event.*;
import com.eventstore.scheduling.eventsourcing.CausationId;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CorrelationId;
import com.eventstore.scheduling.eventsourcing.EventMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.val;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static io.vavr.Predicates.is;

public class EsEventSerde {
    public String prefix = "doctorday";

    public Try<EventData> serialize(Object data, CommandMetadata metadata) {
        val metadataNode = objectMapper.createObjectNode();
        metadataNode.put("correlationId", metadata.getCorrelationId().getValue());
        metadataNode.put("causationId", metadata.getCausationId().getValue());

        val node = objectMapper.createObjectNode();
        return Match(data)
                .of(
                        Case(
                                $(instanceOf(DayScheduled.class)),
                                scheduled -> {
                                    node.put("dayId", scheduled.getDayId().getValue());
                                    node.put("doctorId", scheduled.getDoctorId().getValue());
                                    node.put("date", scheduled.getDate().toString());
                                    return toProposedEvent(prefix + "-day-scheduled", node, metadataNode);
                                }),
                        Case(
                                $(instanceOf(DayScheduleArchived.class)),
                                dayScheduleArchived -> {
                                    node.put("dayId", dayScheduleArchived.getDayId().getValue());
                                    return toProposedEvent(prefix + "-day-schedule-archived", node, metadataNode);
                                }),
                        Case(
                                $(instanceOf(SlotScheduled.class)),
                                slotScheduled -> {
                                    node.put("dayId", slotScheduled.getDayId().getValue());
                                    node.put("slotId", slotScheduled.getSlotId().getValue());
                                    node.put("startDateTime", slotScheduled.getStartDateTime().toString());
                                    node.put("durationInSeconds", slotScheduled.getDuration().getSeconds());
                                    return toProposedEvent(prefix + "-slot-scheduled", node, metadataNode);
                                }),
                        Case(
                                $(instanceOf(SlotBooked.class)),
                                slotBooked -> {
                                    node.put("dayId", slotBooked.getDayId().getValue());
                                    node.put("slotId", slotBooked.getSlotId().getValue());
                                    node.put("patientId", slotBooked.getPatientId().getValue());
                                    return toProposedEvent(prefix + "-slot-booked", node, metadataNode);
                                }),
                        Case(
                                $(instanceOf(SlotBookingCancelled.class)),
                                slotBookingCancelled -> {
                                    node.put("dayId", slotBookingCancelled.getDayId().getValue());
                                    node.put("slotId", slotBookingCancelled.getSlotId().getValue());
                                    node.put("reason", slotBookingCancelled.getReason());
                                    return toProposedEvent(prefix + "-slot-booking-cancelled", node, metadataNode);
                                }),
                        Case(
                                $(instanceOf(SlotScheduleCancelled.class)),
                                slotCancelled -> {
                                    node.put("dayId", slotCancelled.getDayId().getValue());
                                    node.put("slotId", slotCancelled.getSlotId().getValue());
                                    return toProposedEvent(prefix + "-slot-cancelled", node, metadataNode);
                                }),
                        Case(
                                $(instanceOf(DayScheduleCancelled.class)),
                                dayScheduleCancelled -> {
                                    node.put("dayId", dayScheduleCancelled.getDayId().getValue());
                                    node.put("reason", dayScheduleCancelled.getReason());
                                    return toProposedEvent(prefix + "-day-schedule-cancelled", node, metadataNode);
                                }),
                        Case(
                                $(instanceOf(CalendarDayStarted.class)),
                                calendarDayStarted -> {
                                    node.put("date", calendarDayStarted.getDate().toString());
                                    return toProposedEvent(prefix + "-calendar-day-started", node, metadataNode);
                                }));
    }

    @SneakyThrows
    public Tuple2<Object, CommandMetadata> deserialize(ResolvedEvent resolvedEvent) {
        RecordedEvent event = resolvedEvent.getEvent();
        val data = objectMapper.readTree(event.getEventData());
        val deserialized =
                Match(event.getEventType())
                        .of(
                                Case(
                                        $(is(prefix + "-day-scheduled")),
                                        () ->
                                                new DayScheduled(
                                                        new DayId(data.get("dayId").asText()),
                                                        new DoctorId(data.get("doctorId").asText()),
                                                        LocalDate.parse(data.get("date").asText()))),
                                Case(
                                        $(is(prefix + "-day-schedule-archived")),
                                        () -> new DayScheduleArchived(new DayId(data.get("dayId").asText()))),
                                Case(
                                        $(is(prefix + "-slot-scheduled")),
                                        () ->
                                                new SlotScheduled(
                                                        new SlotId(data.get("slotId").asText()),
                                                        new DayId(data.get("dayId").asText()),
                                                        LocalDateTime.parse(data.get("startDateTime").asText()),
                                                        Duration.ofSeconds(data.get("durationInSeconds").asInt()))),
                                Case(
                                        $(is(prefix + "-slot-booked")),
                                        () ->
                                                new SlotBooked(
                                                        new DayId(data.get("dayId").asText()),
                                                        new SlotId(data.get("slotId").asText()),
                                                        new PatientId(data.get("patientId").asText()))),
                                Case(
                                        $(is(prefix + "-slot-booking-cancelled")),
                                        () ->
                                                new SlotBookingCancelled(
                                                        new DayId(data.get("dayId").asText()),
                                                        new SlotId(data.get("slotId").asText()),
                                                        data.get("reason").asText())),
                                Case(
                                        $(is(prefix + "-slot-cancelled")),
                                        () -> new SlotScheduleCancelled(
                                                new DayId(data.get("dayId").asText()), new SlotId(data.get("slotId").asText()))),
                                Case(
                                        $(is(prefix + "-day-schedule-cancelled")),
                                        () ->
                                                new DayScheduleCancelled(
                                                        new DayId(data.get("dayId").asText()),
                                                        data.get("reason").asText())),
                                Case(
                                        $(is(prefix + "-calendar-day-started")),
                                        () ->
                                                new CalendarDayStarted(LocalDate.parse(data.get("date").asText()))));
        val metadata = objectMapper.readTree(event.getUserMetadata());
        val correlationId = new CorrelationId(metadata.get("correlationId").asText());
        val causationId = new CausationId(metadata.get("causationId").asText());
        val eventMetadata = new CommandMetadata(correlationId, causationId);

        return new Tuple2(deserialized, eventMetadata);
    }

    ObjectMapper objectMapper = new ObjectMapper();

    private Try<EventData> toProposedEvent(
            String eventType, ObjectNode data, ObjectNode metadata) {
        return Try.of(
                () ->
                        new EventData(
                                UUID.randomUUID(),
                                eventType,
                                "application/json",
                                objectMapper.writeValueAsBytes(data),
                                objectMapper.writeValueAsBytes(metadata)));
    }
}
