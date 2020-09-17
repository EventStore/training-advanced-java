package com.eventstore.scheduling.eventsourcing;

import io.vavr.collection.List;

public interface ColdStorage {
  void saveAll(String aggregateId, List<Object> events);
}
