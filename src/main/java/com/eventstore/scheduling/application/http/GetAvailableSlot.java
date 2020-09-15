package com.eventstore.scheduling.application.http;

import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class GetAvailableSlot {
  private final @NonNull String dayId;
  private final @NonNull String slotId;
  private final @NonNull LocalDate date;
  private final @NonNull LocalTime time;
  private final @NonNull String duration;

  public static GetAvailableSlot fromDomain(AvailableSlot availableSlot) {
    return new GetAvailableSlot(
        availableSlot.getDayId().getValue(),
        availableSlot.getSlotId().getValue(),
        availableSlot.getDate(),
        availableSlot.getTime(),
        availableSlot.getDuration());
  }
}
