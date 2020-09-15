package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.dbclient.ProposedEvent;
import com.eventstore.dbclient.RecordedEvent;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.scheduling.application.eventsourcing.Checkpoint;
import com.eventstore.scheduling.application.eventsourcing.MessageEnvelope;
import io.vavr.control.Try;
import lombok.val;

public class EsCheckpointSerde implements EsSerde<Object> {
  @Override
  public Try<ProposedEvent> serialize(Object data, Object metadata) {
    if (data instanceof Checkpoint) {
      val checkpoint = (Checkpoint) data;
      val node = objectMapper.createObjectNode();
      node.put("checkpoint", checkpoint.getValue());
      return toProposedEvent("checkpoint", node, objectMapper.createObjectNode());
    }
    return null;
  }

  @Override
  public Try<MessageEnvelope<Object>> deserialize(ResolvedEvent resolvedEvent) {
    return Try.of(
        () -> {
          RecordedEvent event = resolvedEvent.getEvent();
          val data = objectMapper.readTree(event.getEventData());
          return toEnvelope(
              new Checkpoint(data.get("checkpoint").asLong()), new Object(), resolvedEvent);
        });
  }
}
