package com.eventstore.scheduling.infrastructure.projections;

import com.eventstore.scheduling.eventsourcing.EventMetadata;
import io.vavr.Tuple2;

import java.util.function.Consumer;

public record EventHandlerEnvelope<T>(
    Class<T> type,
    Consumer<Tuple2<T, EventMetadata>> handler
) {}

