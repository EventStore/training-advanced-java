package com.eventstore.scheduling.eventsourcing;

import lombok.NonNull;

public record SnapshotEnvelope(
    @NonNull Object snapshot,
    @NonNull Version version
) {}
