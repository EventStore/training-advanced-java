package com.eventstore.scheduling.domain.writemodel.doctorday.event;

import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

@Data
public class CalendarDayStarted implements Event {
  private final @NonNull LocalDate date;
}
