package com.eventstore.scheduling.test;

import com.eventstore.scheduling.eventsourcing.AggregateRoot;
import com.eventstore.scheduling.eventsourcing.AggregateStore;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;

public class FakeAggregateStore implements AggregateStore {
    private AggregateRoot aggregate;

    public FakeAggregateStore(AggregateRoot aggregate) {
        this.aggregate = aggregate;
    }

    @Override
    public <T extends AggregateRoot> void save(T aggregate, CommandMetadata metadata) {
        this.aggregate = aggregate;
    }

    @Override
    public <T extends AggregateRoot> T load(T aggregate, String aggregateId) {
        return (T) this.aggregate;
    }
}
