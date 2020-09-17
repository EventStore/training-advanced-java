package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.eventsourcing.Event;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

@Data
public class CalendarDayStarted implements Event {
  private final @NonNull LocalDate date;
}
