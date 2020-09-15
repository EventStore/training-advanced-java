package com.eventstore.scheduling.application.eventsourcing;

import io.vavr.control.Option;

import java.time.Instant;
import java.util.UUID;

public interface MessageHandler<M> {
  default void handle(MessageEnvelope<M> envelope) {
    handle(
        envelope.getData(),
        envelope.getMetadata(),
        envelope.getMessageId(),
        envelope.getOccurredAt(),
        envelope.getVersion(),
        envelope.getStreamPosition());
  }

  void handle(
      Object message,
      M metadata,
      UUID messageId,
      Instant occurredAt,
      Version position,
      Option<Version> streamPosition);
}
