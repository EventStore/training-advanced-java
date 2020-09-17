package com.eventstore.scheduling.domain.doctorday;

import com.eventstore.scheduling.domain.doctorday.SlotId;
import lombok.Data;
import lombok.NonNull;
import lombok.val;

import java.time.Duration;
import java.time.LocalTime;

@Data
public class Slot {
  private final @NonNull SlotId slotId;
  private final @NonNull LocalTime startTime;
  private final @NonNull Duration duration;
  private final @NonNull Boolean booked;

  public boolean overlapsWith(LocalTime otherStartTime, Duration otherDuration) {
    val firstStart = startTime.toSecondOfDay();
    val firstEnd = startTime.plus(duration).toSecondOfDay();
    val secondStart = otherStartTime.toSecondOfDay();
    val secondEnd = otherStartTime.plus(otherDuration).toSecondOfDay();

    return firstStart < secondEnd && secondStart < firstEnd;
  }
}
