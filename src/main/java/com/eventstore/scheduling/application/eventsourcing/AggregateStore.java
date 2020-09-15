package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.scheduling.domain.writemodel.State;

public interface AggregateStore {
  <C, E, Er, S extends State<S, E>> Aggregate<C, E, Er, S> commit(
      Aggregate<C, E, Er, S> aggregate, EventMetadata metatdata);

  <C, E, Er, S extends State<S, E>> Aggregate<C, E, Er, S> reconsititute(
      Aggregate<C, E, Er, S> aggregate);
}
