package com.eventstore.scheduling.eventsourcing;

import lombok.NonNull;

public record SubscriptionId(
    @NonNull String value
) {}
