package com.eventstore.scheduling.domain.writemodel.doctorday;

import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

@Data
public class DayId {
  private final @NonNull String value;

  public DayId(@NonNull DoctorId doctorId, @NonNull LocalDate date) {
    this.value = doctorId.getValue() + "_" + date.toString();
  }

  public DayId(@NonNull String value) {
    this.value = value;
  }
}
