package com.eventstore.scheduling.domain.writemodel.doctorday.event;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorId;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

@Data
public class DayScheduled implements Event {
  private final @NonNull DayId dayId;
  private final @NonNull DoctorId doctorId;
  private final @NonNull LocalDate date;
}
