package com.eventstore.scheduling.domain.doctorday;

import lombok.NonNull;

public record DoctorId(
    @NonNull String value
) {}
