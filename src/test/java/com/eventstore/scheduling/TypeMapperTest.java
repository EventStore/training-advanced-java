package com.eventstore.scheduling;

import com.eventstore.dbclient.Position;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.StreamRevision;
import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.infrastructure.eventstore.EsEventSerde;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TypeMapperTest {
    private EsEventSerde eventSerde = new EsEventSerde();

    @SneakyThrows
    @Test
    void checkThatSlotBookingCancelledCorrectlyMapWithDefaultValue() {
        ObjectMapper objectMapper = new ObjectMapper();

        val node = objectMapper.createObjectNode();
        node.put("dayId", "day-id-value");
        node.put("slotId", "slot-id-value");
        node.put("reason", "reason-value");

        val dataBytes = objectMapper.writeValueAsBytes(node);

        val metadataNode = objectMapper.createObjectNode();
        metadataNode.put("correlationId", "correlation-id-value");
        metadataNode.put("causationId", "causation-id-value");

        val metadataBytes = objectMapper.writeValueAsBytes(metadataNode);

        HashMap<String, String> systemMetadata = new HashMap<>();
        systemMetadata.put("type", "doctorday-slot-booking-cancelled");
        systemMetadata.put("created", "0");

        val event = new ResolvedEvent(new RecordedEvent(
                "", StreamRevision.START, UUID.randomUUID(), Position.START, systemMetadata, dataBytes, metadataBytes
        ), null);

        assertEquals(eventSerde.deserialize(event)._1, new SlotBookingCancelled(new DayId("day-id-value"), new SlotId("slot-id-value"), "reason-value", "unknown"));
    }

    @SneakyThrows
    @Test
    void checkThatSlotBookingCancelledCorrectlyMapWithProvidedValue() {
        ObjectMapper objectMapper = new ObjectMapper();

        val node = objectMapper.createObjectNode();
        node.put("dayId", "day-id-value");
        node.put("slotId", "slot-id-value");
        node.put("reason", "reason-value");
        node.put("requestedBy", "doctor");

        val dataBytes = objectMapper.writeValueAsBytes(node);

        val metadataNode = objectMapper.createObjectNode();
        metadataNode.put("correlationId", "correlation-id-value");
        metadataNode.put("causationId", "causation-id-value");

        val metadataBytes = objectMapper.writeValueAsBytes(metadataNode);

        HashMap<String, String> systemMetadata = new HashMap<>();
        systemMetadata.put("type", "doctorday-slot-booking-cancelled");
        systemMetadata.put("created", "0");

        val event = new ResolvedEvent(new RecordedEvent(
                "", StreamRevision.START, UUID.randomUUID(), Position.START, systemMetadata, dataBytes, metadataBytes
        ), null);

        assertEquals(eventSerde.deserialize(event)._1, new SlotBookingCancelled(new DayId("day-id-value"), new SlotId("slot-id-value"), "reason-value", "doctor"));
    }
}
