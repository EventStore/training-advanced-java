package com.eventstore.scheduling.infrastructure.inmemory;

import com.eventstore.scheduling.application.eventsourcing.ColdStorage;
import com.eventstore.scheduling.application.eventsourcing.EventMetadata;
import com.eventstore.scheduling.application.eventsourcing.MessageEnvelope;
import com.eventstore.scheduling.domain.writemodel.AggregateId;
import io.vavr.collection.List;

public class InMemoryColdStorage implements ColdStorage {
  public List<MessageEnvelope<EventMetadata>> events = List.empty();

  @Override
  public void saveAll(AggregateId aggregateId, List<MessageEnvelope<EventMetadata>> newEvents) {
    events = events.appendAll(newEvents);
  }
}
