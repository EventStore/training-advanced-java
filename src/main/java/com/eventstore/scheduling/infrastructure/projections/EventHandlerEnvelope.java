package com.eventstore.scheduling.infrastructure.projections;

import lombok.Data;

import java.util.function.Consumer;

@Data
public class EventHandlerEnvelope {
    private final Class type;

    private final Consumer<Object> handler;
}

