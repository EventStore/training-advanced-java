package com.eventstore.scheduling.eventsourcing;

import lombok.NonNull;

public record Version(
    @NonNull Long value
)
{
    public static Version fresh = new Version(-1L);

    public Version incrementBy(int length) {
        return new Version(value + length);
    }
}
