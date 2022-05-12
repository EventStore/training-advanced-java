package com.eventstore.scheduling.domain.doctorday;

import lombok.NonNull;

public record DoctorDayId(
    @NonNull DayId dayId
)
{
    @Override
    public String toString() {
        return "doctorday-%s".formatted(dayId.value());
    }
}
