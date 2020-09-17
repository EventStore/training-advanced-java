package com.eventstore.scheduling.eventsourcing;

public interface AggregateStore {
  <T extends AggregateRoot> void save(T aggregate, CommandMetadata metadata);
  <T extends AggregateRoot> T load(T aggregate, String aggregateId);
}
