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
import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.val;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static io.vavr.API.None;

public class EsEventSerde {
    public Try<EventData> serialize(Object data, CommandMetadata metadata) {
        val metadataNode = objectMapper.createObjectNode();
        metadataNode.put("correlationId", metadata.correlationId().value());
        metadataNode.put("causationId", metadata.causationId().value());

        val node = objectMapper.createObjectNode();
        return switch(data) {
            case DayScheduled scheduled -> {
                node.put("dayId", scheduled.dayId().value());
                node.put("doctorId", scheduled.doctorId().value());
                node.put("date", scheduled.date().toString());
                yield toProposedEvent("doctorday-day-scheduled", node, metadataNode);
            }
            case DayScheduleArchived dayScheduleArchived -> {
                node.put("dayId", dayScheduleArchived.dayId().value());
                yield toProposedEvent("doctorday-day-schedule-archived", node, metadataNode);
            }
            case SlotScheduled slotScheduled -> {
                node.put("dayId", slotScheduled.dayId().value());
                node.put("slotId", slotScheduled.slotId().value());
                node.put("startDateTime", slotScheduled.startDateTime().toString());
                node.put("durationInSeconds", slotScheduled.duration().getSeconds());
                yield toProposedEvent("doctorday-slot-scheduled", node, metadataNode);
            }
            case SlotBooked slotBooked -> {
                node.put("dayId", slotBooked.dayId().value());
                node.put("slotId", slotBooked.slotId().value());
                node.put("patientId", slotBooked.patientId().value());
                yield toProposedEvent("doctorday-slot-booked", node, metadataNode);
            }
            case SlotBookingCancelled slotBookingCancelled -> {
                node.put("dayId", slotBookingCancelled.dayId().value());
                node.put("slotId", slotBookingCancelled.slotId().value());
                node.put("reason", slotBookingCancelled.reason());
                yield toProposedEvent("doctorday-slot-booking-cancelled", node, metadataNode);
            }
            case SlotScheduleCancelled slotCancelled -> {
                node.put("dayId", slotCancelled.dayId().value());
                node.put("slotId", slotCancelled.slotId().value());
                yield toProposedEvent("doctorday-slot-cancelled", node, metadataNode);
            }
            case DayScheduleCancelled dayScheduleCancelled -> {
                node.put("dayId", dayScheduleCancelled.dayId().value());
                node.put("reason", dayScheduleCancelled.reason());
                yield toProposedEvent("doctorday-day-schedule-cancelled", node, metadataNode);
            }
            case CalendarDayStarted calendarDayStarted -> {
                node.put("date", calendarDayStarted.date().toString());
                yield  toProposedEvent("doctorday-calendar-day-started", node, metadataNode);
            }
            default -> throw new IllegalStateException("Unexpected value: %s".formatted(data));
        };
    }

    @SneakyThrows
    public Tuple2<Object, EventMetadata> deserialize(ResolvedEvent resolvedEvent) {
        RecordedEvent event = resolvedEvent.getEvent();
        val data = objectMapper.readTree(event.getEventData());
        val deserialized =
                switch (event.getEventType()) {
                    case "doctorday-day-scheduled" ->
                        new DayScheduled(
                            new DayId(data.get("dayId").asText()),
                            new DoctorId(data.get("doctorId").asText()),
                            LocalDate.parse(data.get("date").asText())
                        );
                    case "doctorday-day-schedule-archived" ->
                        new DayScheduleArchived(new DayId(data.get("dayId").asText()));
                    case "doctorday-slot-scheduled" ->
                        new SlotScheduled(
                            new SlotId(data.get("slotId").asText()),
                            new DayId(data.get("dayId").asText()),
                            LocalDateTime.parse(data.get("startDateTime").asText()),
                            Duration.ofSeconds(data.get("durationInSeconds").asInt())
                        );
                    case "doctorday-slot-booked" ->
                        new SlotBooked(
                            new DayId(data.get("dayId").asText()),
                            new SlotId(data.get("slotId").asText()),
                            new PatientId(data.get("patientId").asText())
                        );
                    case "doctorday-slot-booking-cancelled" ->
                        new SlotBookingCancelled(
                            new DayId(data.get("dayId").asText()),
                            new SlotId(data.get("slotId").asText()),
                            data.get("reason").asText()
                        );
                    case "doctorday-slot-cancelled" ->
                        new SlotScheduleCancelled(
                            new DayId(data.get("dayId").asText()),
                            new SlotId(data.get("slotId").asText())
                        );
                    case "doctorday-day-schedule-cancelled" ->
                        new DayScheduleCancelled(
                            new DayId(data.get("dayId").asText()),
                            data.get("reason").asText()
                        );
                    case "doctorday-calendar-day-started" ->
                        new CalendarDayStarted(LocalDate.parse(data.get("date").asText()));
                    default -> throw new IllegalStateException("Unexpected value: %s".formatted(event.getEventType()));
                };

        val metadata = objectMapper.readTree(event.getUserMetadata());
        val correlationId = new CorrelationId(metadata.get("correlationId").asText());
        val causationId = new CausationId(metadata.get("causationId").asText());
        val eventMetadata = new EventMetadata(correlationId, causationId, event.getPosition().getCommitUnsigned(), None());

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
