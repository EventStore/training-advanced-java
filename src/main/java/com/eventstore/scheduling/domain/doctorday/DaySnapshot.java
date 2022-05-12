package com.eventstore.scheduling.domain.doctorday;

import io.vavr.collection.List;
import lombok.NonNull;

import java.time.LocalDate;

public record DaySnapshot(
    @NonNull List<Slot> slots,
    @NonNull Boolean isArchived,
    @NonNull Boolean isCancelled,
    @NonNull Boolean isScheduled,
    @NonNull DayId dayId,
    @NonNull LocalDate date
) {}
