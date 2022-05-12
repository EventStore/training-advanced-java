package com.eventstore.scheduling.eventsourcing;

import lombok.NonNull;

public record CommandEnvelope(
    @NonNull Object command,
    @NonNull CommandMetadata metadata
) {}
