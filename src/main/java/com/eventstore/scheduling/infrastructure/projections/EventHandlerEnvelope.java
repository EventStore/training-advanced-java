package com.eventstore.scheduling.infrastructure.projections;

import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.EventMetadata;
import io.vavr.Tuple2;
import lombok.Data;

import java.util.function.Consumer;

@Data
public class EventHandlerEnvelope<T> {
    private final Class<T> type;

    private final Consumer<Tuple2<T, EventMetadata>> handler;
}

