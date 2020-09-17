package com.eventstore.scheduling.domain.service;

import com.eventstore.scheduling.domain.doctorday.Day;
import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;

public interface DayRepository {
    Day get(DayId dayId);
    void save(Day aggregate, CommandMetadata metadata);
}
