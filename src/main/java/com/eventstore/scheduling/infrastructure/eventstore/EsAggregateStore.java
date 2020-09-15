package com.eventstore.scheduling.infrastructure.eventstore;

import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.writemodel.State;
import io.vavr.collection.List;
import lombok.SneakyThrows;
import lombok.val;

import static io.vavr.API.Some;

public class EsAggregateStore implements AggregateStore {
  private final EventStoreClient<EventMetadata> eventStoreClient;

  public EsAggregateStore(EventStoreClient<EventMetadata> eventStoreClient) {
    this.eventStoreClient = eventStoreClient;
  }

  @Override
  public <C, E, Er, S extends State<S, E>> Aggregate<C, E, Er, S> commit(
      Aggregate<C, E, Er, S> aggregate, EventMetadata metatdata) {
    val changes = aggregate.getChanges();
    if (aggregate.isNew()) {
      eventStoreClient.createNewStream(aggregate.getId().toString(), changes, metatdata);
    } else {
      eventStoreClient.appendToStream(
          aggregate.getId().toString(), changes, metatdata, aggregate.getVersion());
    }
    return aggregate.markAsCommitted();
  }

  @SneakyThrows
  @Override
  public <C, E, Er, S extends State<S, E>> Aggregate<C, E, Er, S> reconsititute(
      Aggregate<C, E, Er, S> aggregate) {
    List<E> events =
        (List<E>)
            eventStoreClient
                .readFromStream(
                    aggregate.getId().toString(), Some(aggregate.getVersion().incrementBy(1)))
                .map(MessageEnvelope::getData);

    return aggregate.reconstitute(events);
  }
}
