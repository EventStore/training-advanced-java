package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.ProposedEvent;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.scheduling.application.eventsourcing.CausationId;
import com.eventstore.scheduling.application.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.application.eventsourcing.CorrelationId;
import com.eventstore.scheduling.application.eventsourcing.MessageEnvelope;
import com.eventstore.scheduling.domain.writemodel.AggregateId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.Archive;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.CancelSlotBooking;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.ScheduleDay;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.ScheduleSlot;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.val;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;
import static io.vavr.Predicates.is;

public class EsCommandSerde implements EsSerde<CommandMetadata> {
  @Override
  public Try<ProposedEvent> serialize(Object data, CommandMetadata metadata) {
    val metadataNode = objectMapper.createObjectNode();
    metadataNode.put("correlationId", metadata.getCorrelationId().getValue());
    metadataNode.put("causationId", metadata.getCausationId().getValue());
    metadataNode.put("aggregateIdType", metadata.getAggregateId().getType());
    metadataNode.put("aggregateIdValue", metadata.getAggregateId().getValue());

    val node = objectMapper.createObjectNode();
    return Match(data)
        .of(
            Case(
                $(instanceOf(ScheduleDay.class)),
                scheduleDay -> {
                  node.put("doctorId", scheduleDay.getDoctorId().getValue());
                  node.put("date", scheduleDay.getDate().toString());
                  val slotsNode = objectMapper.createArrayNode();
                  scheduleDay
                      .getSlots()
                      .forEach(
                          slot -> {
                            val slotNode = objectMapper.createObjectNode();
                            slotNode.put("startTime", slot.getStartTime().toString());
                            slotNode.put("duration", slot.getDuration().toString());
                            slotsNode.add(slotNode);
                          });
                  node.set("slots", slotsNode);
                  return toProposedEvent("command-schedule-day", node, metadataNode);
                }),
            Case(
                $(instanceOf(CancelSlotBooking.class)),
                cancelSlotBooking -> {
                  node.put("slotId", cancelSlotBooking.getSlotId().getValue());
                  node.put("reason", cancelSlotBooking.getReason());
                  return toProposedEvent("command-cancel-slot-booking", node, metadataNode);
                }),
            Case(
                $(instanceOf(Archive.class)),
                cancelSlotBooking -> toProposedEvent("command-archive", node, metadataNode)));
  }

  @Override
  public Try<MessageEnvelope<CommandMetadata>> deserialize(ResolvedEvent resolvedEvent) {
    return Try.of(
        () -> {
          RecordedEvent event = resolvedEvent.getEvent();
          val data = objectMapper.readTree(event.getEventData());
          val deserialized =
              Match(event.getEventType())
                  .of(
                      Case(
                          $(is("command-schedule-day")),
                          () ->
                              new ScheduleDay(
                                  new DoctorId(data.get("doctorId").asText()),
                                  LocalDate.parse(data.get("date").asText()),
                                  List.ofAll(data.get("slots"))
                                      .map(
                                          node ->
                                              new ScheduleSlot(
                                                  LocalTime.parse(node.get("startTime").asText()),
                                                  Duration.parse(node.get("duration").asText()))))),
                      Case(
                          $(is("command-cancel-slot-booking")),
                          () ->
                              new CancelSlotBooking(
                                  new SlotId(data.get("slotId").asText()),
                                  data.get("reason").asText())),
                      Case($(is("command-archive")), Archive::new));
          val metadata = objectMapper.readTree(event.getUserMetadata());
          val correlationId = new CorrelationId(metadata.get("correlationId").asText());
          val causationId = new CausationId(metadata.get("causationId").asText());
          val aggregateId =
              new AggregateId(
                  metadata.get("aggregateIdValue").asText(),
                  metadata.get("aggregateIdType").asText());
          val eventMetadata = new CommandMetadata(correlationId, causationId, aggregateId);

          return toEnvelope(deserialized, eventMetadata, resolvedEvent);
        });
  }
}
