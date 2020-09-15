package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.ProposedEvent;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.*;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.val;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static io.vavr.Predicates.is;

public class EsSnapshotSerde implements EsSerde<SnapshotMetadata> {
  private final String prefix = "snapshot-doctorday";

  @Override
  public Try<ProposedEvent> serialize(Object data, SnapshotMetadata metadata) {
    val metadataNode = objectMapper.createObjectNode();
    metadataNode.put("correlationId", metadata.getCorrelationId().getValue());
    metadataNode.put("causationId", metadata.getCausationId().getValue());
    metadataNode.put("version", metadata.getVersion().getValue());

    val node = objectMapper.createObjectNode();
    return Match(data)
        .of(
            Case(
                $(instanceOf(Scheduled.class)),
                scheduled -> {
                  node.put("dayId", scheduled.getDayId().getValue());
                  node.put("date", scheduled.getDate().toString());
                  val slotsNode = objectMapper.createArrayNode();
                  scheduled
                      .getAllSlots()
                      .forEach(
                          slot -> {
                            val slotNode = objectMapper.createObjectNode();
                            slotNode.put("slotId", slot.getSlotId().getValue());
                            slotNode.put("startTime", slot.getStartTime().toString());
                            slotNode.put("duration", slot.getDuration().getSeconds());
                            slotNode.put("booked", slot.getBooked());
                            slotsNode.add(slotNode);
                          });
                  node.set("slots", slotsNode);
                  return toProposedEvent(prefix + "-scheduled", node, metadataNode);
                }),
            Case(
                $(instanceOf(Cancelled.class)),
                cancelled -> {
                  node.put("dayId", cancelled.getDayId().getValue());
                  return toProposedEvent(prefix + "-cancelled", node, metadataNode);
                }),
            Case(
                $(instanceOf(Archived.class)),
                archived -> {
                  return toProposedEvent(prefix + "-archived", node, metadataNode);
                }));
  }

  @Override
  public Try<MessageEnvelope<SnapshotMetadata>> deserialize(ResolvedEvent resolvedEvent) {
    return Try.of(
        () -> {
          RecordedEvent event = resolvedEvent.getEvent();
          val data = objectMapper.readTree(event.getEventData());
          val deserialized =
              Match(event.getEventType())
                  .of(
                      Case(
                          $(is(prefix + "-scheduled")),
                          () ->
                              new Scheduled(
                                  new DayId(data.get("dayId").asText()),
                                  LocalDate.parse(data.get("date").asText()),
                                  new Slots(
                                      List.ofAll(data.get("slots"))
                                          .map(
                                              node ->
                                                  new Slot(
                                                      new SlotId(node.get("slotId").asText()),
                                                      LocalTime.parse(
                                                          node.get("startTime").asText()),
                                                      Duration.ofSeconds(
                                                          node.get("duration").asLong()),
                                                      node.get("booked").asBoolean()))))),
                      Case(
                          $(is(prefix + "-cancelled")),
                          () -> new Cancelled(new DayId(data.get("dayId").asText()))),
                      Case($(is(prefix + "-archived")), Archived::new));
          val metadata = objectMapper.readTree(event.getUserMetadata());
          val correlationId = new CorrelationId(metadata.get("correlationId").asText());
          val causationId = new CausationId(metadata.get("causationId").asText());
          val version = new Version(metadata.get("version").asLong());
          val eventMetadata = new SnapshotMetadata(correlationId, causationId, version);

          return toEnvelope(deserialized, eventMetadata, resolvedEvent);
        });
  }
}
