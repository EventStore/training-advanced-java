package com.eventstore.scheduling.domain.doctorday.command;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.eventsourcing.Command;
import lombok.NonNull;

public record CancelSlotBooking(
   @NonNull DayId dayId,
   @NonNull SlotId slotId,
   @NonNull String reason
) implements Command {}
