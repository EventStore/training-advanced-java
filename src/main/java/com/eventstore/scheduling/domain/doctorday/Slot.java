package com.eventstore.scheduling.domain.doctorday;

import lombok.NonNull;
import lombok.val;

import java.time.Duration;
import java.time.LocalTime;

public record Slot(
    @NonNull SlotId slotId,
    @NonNull LocalTime startTime,
    @NonNull Duration duration,
    @NonNull Boolean booked
)
{
    public boolean overlapsWith(LocalTime otherStartTime, Duration otherDuration) {
        val firstStart = startTime.toSecondOfDay();
        val firstEnd = startTime.plus(duration).toSecondOfDay();
        val secondStart = otherStartTime.toSecondOfDay();
        val secondEnd = otherStartTime.plus(otherDuration).toSecondOfDay();

        return firstStart < secondEnd && secondStart < firstEnd;
    }
}
