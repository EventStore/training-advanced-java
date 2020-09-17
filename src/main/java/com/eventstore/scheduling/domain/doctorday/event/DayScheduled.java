package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DoctorId;
import com.eventstore.scheduling.eventsourcing.Event;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

@Data
public class DayScheduled implements Event {
  private final @NonNull DayId dayId;
  private final @NonNull DoctorId doctorId;
  private final @NonNull LocalDate date;
}
