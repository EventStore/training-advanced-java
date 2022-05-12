package com.eventstore.scheduling.eventsourcing;

import io.vavr.control.Option;
import lombok.NonNull;

import java.time.Instant;
import java.util.UUID;

public record MessageEnvelope<M>(
    @NonNull Object data,
    @NonNull M metadata,
    @NonNull UUID messageId,
    @NonNull Instant occurredAt,
    @NonNull Version version,
    @NonNull Option<Version> streamPosition
) {}
