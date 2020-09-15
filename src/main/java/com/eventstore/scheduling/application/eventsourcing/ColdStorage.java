package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.scheduling.domain.writemodel.AggregateId;
import io.vavr.collection.List;

public interface ColdStorage {
  void saveAll(AggregateId aggregateId, List<MessageEnvelope<EventMetadata>> events);
}
