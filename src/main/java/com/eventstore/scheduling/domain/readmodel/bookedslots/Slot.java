package com.eventstore.scheduling.domain.readmodel.bookedslots;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import lombok.NonNull;

import java.time.Month;

public record Slot(
    @NonNull SlotId slotId,
    @NonNull DayId dayId,
    @NonNull Month month
) {}
