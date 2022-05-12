package com.eventstore.scheduling.domain.doctorday;

import lombok.NonNull;

import java.time.LocalDate;

public record DayId(
    @NonNull String value
)
{
  public DayId(@NonNull DoctorId doctorId, @NonNull LocalDate date) {
    this("%s_%s".formatted(doctorId.value(), date));
  }
}
