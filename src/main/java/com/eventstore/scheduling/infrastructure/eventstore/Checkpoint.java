package com.eventstore.scheduling.infrastructure.eventstore;

import lombok.NonNull;

public record Checkpoint(
    @NonNull Long value
) {}
