package com.eventstore.scheduling.domain.doctorday.event;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.eventstore.scheduling.eventsourcing.Event;
import lombok.NonNull;

public record SlotBookingCancelled(
    @NonNull DayId dayId,
    @NonNull SlotId slotId,
    @NonNull String reason
) implements Event {}
