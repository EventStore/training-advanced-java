package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DoctorId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.command.ArchiveDaySchedule;
import com.eventstore.scheduling.domain.doctorday.command.CancelSlotBooking;
import com.eventstore.scheduling.domain.doctorday.command.ScheduleDay;
import com.eventstore.scheduling.domain.doctorday.command.ScheduleSlot;
import com.eventstore.scheduling.eventsourcing.CausationId;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

public class EsCommandSerde {
    ObjectMapper objectMapper = new ObjectMapper();

    public EventData serialize(Object data, CommandMetadata metadata) {
        val metadataNode = objectMapper.createObjectNode();
        metadataNode.put("correlationId", metadata.correlationId().value());
        metadataNode.put("causationId", metadata.causationId().value());

        val node = objectMapper.createObjectNode();
        return Match(data)
                .of(
                        Case(
                                $(instanceOf(ScheduleDay.class)),
                                scheduleDay -> {
                                    node.put("dayId", scheduleDay.dayId().value());
                                    node.put("doctorId", scheduleDay.doctorId().value());
                                    node.put("date", scheduleDay.date().toString());
                                    val slotsNode = objectMapper.createArrayNode();
                                    scheduleDay
                                            .slots()
                                            .forEach(
                                                    slot -> {
                                                        val slotNode = objectMapper.createObjectNode();
                                                        slotNode.put("startTime", slot.startTime().toString());
                                                        slotNode.put("duration", slot.duration().toString());
                                                        slotsNode.add(slotNode);
                                                    });
                                    node.set("slots", slotsNode);
                                    return toProposedEvent("command-schedule-day", node, metadataNode);
                                }),
                        Case(
                                $(instanceOf(CancelSlotBooking.class)),
                                cancelSlotBooking -> {
                                    node.put("dayId", cancelSlotBooking.dayId().value());
                                    node.put("slotId", cancelSlotBooking.slotId().value());
                                    node.put("reason", cancelSlotBooking.reason());
                                    return toProposedEvent("command-cancel-slot-booking", node, metadataNode);
                                }),
                        Case(
                                $(instanceOf(ArchiveDaySchedule.class)),
                                archiveDaySchedule -> {
                                    node.put("dayId", archiveDaySchedule.dayId().value());
                                    return toProposedEvent("command-archive", node, metadataNode);
                                }));
    }

    @SneakyThrows
    private EventData toProposedEvent(
            String eventType, ObjectNode data, ObjectNode metadata) {
        return new EventData(
                UUID.randomUUID(),
                eventType,
                "application/json",
                objectMapper.writeValueAsBytes(data),
                objectMapper.writeValueAsBytes(metadata));
    }

    @SneakyThrows
    public Tuple2<Object, CommandMetadata> deserialize(ResolvedEvent resolvedEvent) {
        RecordedEvent event = resolvedEvent.getEvent();
        val data = objectMapper.readTree(event.getEventData());
        val deserialized =
            switch (event.getEventType()) {
                case "command-schedule-day" ->
                    new ScheduleDay(
                        new DoctorId(data.get("doctorId").asText()),
                        LocalDate.parse(data.get("date").asText()),
                        List.ofAll(data.get("slots"))
                            .map(node ->
                                new ScheduleSlot(
                                    new DayId(node.get("dayId").asText()),
                                    LocalTime.parse(node.get("startTime").asText()),
                                    Duration.parse(node.get("duration").asText())
                                )
                            )
                    );
                case "command-cancel-slot-booking" ->
                    new CancelSlotBooking(
                        new DayId(data.get("dayId").asText()),
                        new SlotId(data.get("slotId").asText()),
                        data.get("reason").asText()
                    );
                case "command-archive" ->
                    new ArchiveDaySchedule(new DayId(data.get("dayId").asText()));

                default -> throw new IllegalStateException("Unexpected value: %s".formatted(event.getEventType()));
            };
        val metadata = objectMapper.readTree(event.getUserMetadata());
        val correlationId = new CorrelationId(metadata.get("correlationId").asText());
        val causationId = new CausationId(metadata.get("causationId").asText());
        val eventMetadata = new CommandMetadata(correlationId, causationId);

        return Tuple(deserialized, eventMetadata);
    }
}
