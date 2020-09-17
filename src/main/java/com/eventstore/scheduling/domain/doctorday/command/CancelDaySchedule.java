package com.eventstore.scheduling.domain.doctorday.command;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.eventsourcing.Command;
import lombok.Data;
import lombok.NonNull;

@Data
public class CancelDaySchedule implements Command {
  private final @NonNull DayId dayId;
}
