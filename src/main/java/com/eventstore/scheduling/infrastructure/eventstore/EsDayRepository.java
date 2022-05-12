package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.scheduling.domain.doctorday.Day;
import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.service.DayRepository;
import com.eventstore.scheduling.eventsourcing.AggregateStore;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;

public class EsDayRepository implements DayRepository {
    private final AggregateStore aggregateStore;

    public EsDayRepository(AggregateStore aggregateStore) {
        this.aggregateStore = aggregateStore;
    }

    @Override
    public Day get(DayId dayId) {
        return aggregateStore.load(new Day(), dayId.value());
    }

    @Override
    public void save(Day aggregate, CommandMetadata metadata) {
        aggregateStore.save(aggregate, metadata);
    }
}
