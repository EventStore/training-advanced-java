package com.eventstore.scheduling.domain.doctorday.command;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DoctorId;
import com.eventstore.scheduling.eventsourcing.Command;
import lombok.Data;
import lombok.NonNull;

import java.time.Duration;
import java.time.LocalTime;

@Data
public class ScheduleSlot implements Command {
  private final @NonNull DayId dayId;
  private final @NonNull LocalTime startTime;
  private final @NonNull Duration duration;
}
