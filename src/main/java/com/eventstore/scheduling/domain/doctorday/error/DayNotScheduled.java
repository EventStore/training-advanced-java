package com.eventstore.scheduling.domain.doctorday.error;

import com.eventstore.scheduling.eventsourcing.Error;
import lombok.Data;

@Data
public class DayNotScheduled extends Error {}
