package com.eventstore.scheduling.domain.doctorday;

import lombok.Data;
import lombok.NonNull;

@Data
public class DoctorId {
  private final @NonNull String value;
}
