package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.ProposedEvent;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.StreamRevision;
import com.eventstore.scheduling.application.eventsourcing.MessageEnvelope;
import com.eventstore.scheduling.application.eventsourcing.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Option;
import io.vavr.control.Try;

import java.util.UUID;

public interface EsSerde<M> {
  ObjectMapper objectMapper = new ObjectMapper();

  Try<ProposedEvent> serialize(Object data, M metadata);

  Try<MessageEnvelope<M>> deserialize(ResolvedEvent resolvedEvent);

  default Try<ProposedEvent> toProposedEvent(
      String eventType, ObjectNode data, ObjectNode metadata) {
    return Try.of(
        () ->
            new ProposedEvent(
                UUID.randomUUID(),
                eventType,
                "application/json",
                objectMapper.writeValueAsBytes(data),
                objectMapper.writeValueAsBytes(metadata)));
  }

  default MessageEnvelope<M> toEnvelope(Object event, M metadata, ResolvedEvent resolvedEvent) {
    return new MessageEnvelope<>(
        event,
        metadata,
        resolvedEvent.getEvent().getEventId(),
        resolvedEvent.getEvent().getCreated(),
        new Version(resolvedEvent.getEvent().getStreamRevision().getValueUnsigned()),
        Option.of(resolvedEvent.getLink())
            .map(RecordedEvent::getStreamRevision)
            .map(StreamRevision::getValueUnsigned)
            .map(Version::new));
  }
}
