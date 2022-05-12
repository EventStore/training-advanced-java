package com.eventstore.scheduling.eventsourcing;

import io.vavr.control.Option;
import lombok.NonNull;

public record EventMetadata(
    @NonNull CorrelationId correlationId,
    @NonNull CausationId causationId,
    @NonNull Long position,
    @NonNull Option<String> replayed
) {}
