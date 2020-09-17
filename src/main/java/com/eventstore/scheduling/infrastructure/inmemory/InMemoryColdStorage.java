package com.eventstore.scheduling.infrastructure.inmemory;

import com.eventstore.scheduling.eventsourcing.ColdStorage;
import com.eventstore.scheduling.eventsourcing.EventMetadata;
import com.eventstore.scheduling.eventsourcing.MessageEnvelope;
import io.vavr.collection.List;

public class InMemoryColdStorage implements ColdStorage {
  public List<Object> events = List.empty();

  @Override
  public void saveAll(String aggregateId, List<Object> events) {
    this.events = events;
  }
}
