package com.eventstore.scheduling.domain.doctorday.command;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.eventsourcing.Command;
import lombok.NonNull;

public record CancelDaySchedule(
  @NonNull DayId dayId
) implements Command {}
