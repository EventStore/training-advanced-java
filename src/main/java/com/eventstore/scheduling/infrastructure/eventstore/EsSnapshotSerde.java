package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DaySnapshot;
import com.eventstore.scheduling.domain.doctorday.Slot;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.eventsourcing.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class EsSnapshotSerde {
    private final String eventType = "snapshot-doctorday";

    ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    public EventData serialize(Object data, Long version, CommandMetadata metadata) {
        val metadataNode = objectMapper.createObjectNode();
        metadataNode.put("version", version);
        metadataNode.put("correlationId", metadata.correlationId().value());
        metadataNode.put("causationId", metadata.causationId().value());

        val node = objectMapper.createObjectNode();

        val snapshot = (DaySnapshot) data;

        node.put("dayId", snapshot.dayId().value());
        node.put("date", snapshot.date().toString());
        node.put("archived", snapshot.isArchived().toString());
        node.put("cancelled", snapshot.isCancelled().toString());
        node.put("scheduled", snapshot.isScheduled().toString());
        val slotsNode = objectMapper.createArrayNode();
        snapshot
                .slots()
                .forEach(
                        slot -> {
                            val slotNode = objectMapper.createObjectNode();
                            slotNode.put("slotId", slot.slotId().value());
                            slotNode.put("startTime", slot.startTime().toString());
                            slotNode.put("duration", slot.duration().getSeconds());
                            slotNode.put("booked", slot.booked());
                            slotsNode.add(slotNode);
                        });
        node.set("slots", slotsNode);
        return new EventData(
                UUID.randomUUID(),
                eventType,
                "application/json",
                objectMapper.writeValueAsBytes(node),
                objectMapper.writeValueAsBytes(metadataNode));
    }

    @SneakyThrows
    public SnapshotEnvelope deserialize(ResolvedEvent resolvedEvent) {
        RecordedEvent event = resolvedEvent.getEvent();
        val data = objectMapper.readTree(event.getEventData());
        val deserialized =
                new DaySnapshot(
                        List.ofAll(data.get("slots"))
                                .map(
                                        node ->
                                                new Slot(
                                                        new SlotId(node.get("slotId").asText()),
                                                        LocalTime.parse(
                                                                node.get("startTime").asText()),
                                                        Duration.ofSeconds(
                                                                node.get("duration").asLong()),
                                                        node.get("booked").asBoolean())),
                        data.get("archived").asBoolean(),
                        data.get("cancelled").asBoolean(),
                        data.get("scheduled").asBoolean(),
                        new DayId(data.get("dayId").asText()),
                        LocalDate.parse(data.get("date").asText())
                );

        val metadata = objectMapper.readTree(event.getUserMetadata());

        return new SnapshotEnvelope(deserialized, new Version(metadata.get("version").asLong()));
    }
}
