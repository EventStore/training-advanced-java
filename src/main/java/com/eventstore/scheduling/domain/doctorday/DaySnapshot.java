package com.eventstore.scheduling.domain.doctorday;

import io.vavr.collection.List;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDate;

@Data
public class DaySnapshot {
    private @NonNull List<Slot> slots;
    private @NonNull Boolean isArchived;
    private @NonNull Boolean isCancelled;
    private @NonNull Boolean isScheduled;
    private @NonNull DayId dayId;
    private @NonNull LocalDate date;
}
