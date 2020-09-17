package com.eventstore.scheduling.domain.doctorday;

import lombok.Data;
import lombok.NonNull;

@Data
public class DoctorDayId {
  private @NonNull DayId dayId;

  @Override
  public String toString() {
    return "doctorday-" + dayId.getValue();
  }
}
