package com.eventstore.scheduling.domain.doctorday.command;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DoctorId;
import com.eventstore.scheduling.eventsourcing.Command;
import io.vavr.collection.List;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

@Data
public class ScheduleDay implements Command {
  private final @NonNull DoctorId doctorId;
  private final @NonNull LocalDate date;
  private final @NonNull List<ScheduleSlot> slots;

  public DayId getDayId() {
    return new DayId(doctorId, date);
  }
}
