package com.eventstore.scheduling.eventsourcing;

import io.vavr.control.Option;
import lombok.Data;
import lombok.NonNull;

import java.time.Instant;
import java.util.UUID;

@Data
public class MessageEnvelope<M> {
  private final @NonNull Object data;
  private final @NonNull M metadata;
  private final @NonNull UUID messageId;
  private final @NonNull Instant occurredAt;
  private final @NonNull Version version;
  private final @NonNull Option<Version> streamPosition;
}
