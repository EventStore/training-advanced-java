package com.eventstore.scheduling.domain.doctorday;

import lombok.NonNull;

public record PatientId(
    @NonNull String value
) {}
