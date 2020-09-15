package com.eventstore.scheduling.domain.writemodel.doctorday.state;

import lombok.Data;
import lombok.NonNull;

@Data
public class PatientId {
  private final @NonNull String value;
}
