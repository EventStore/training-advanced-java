package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.scheduling.domain.writemodel.AggregateId;
import com.eventstore.scheduling.domain.writemodel.AggregateLogic;
import com.eventstore.scheduling.domain.writemodel.State;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.Data;
import lombok.NonNull;

@Data
public class Aggregate<C, E, Er, S extends State<S, E>> {
  private final @NonNull AggregateId id;
  private final @NonNull AggregateLogic<C, E, Er, S> aggregateLogic;
  private final @NonNull S state;
  private final @NonNull Version version;
  private final @NonNull List<E> changes;

  public static <C, E, Er, S extends State<S, E>> Aggregate<C, E, Er, S> instance(
      AggregateId id, AggregateLogic<C, E, Er, S> aggregateLogic) {
    return new Aggregate<C, E, Er, S>(
        id, aggregateLogic, aggregateLogic.initialState(), Version.fresh, List.empty());
  }

  public boolean isNew() {
    return version.equals(Version.fresh);
  }

  public Either<Er, Aggregate<C, E, Er, S>> handle(C command) {
    return aggregateLogic
        .apply(state, command)
        .map(
            (newEvents) ->
                new Aggregate<>(
                    id,
                    aggregateLogic,
                    newEvents.foldLeft(state, State::apply),
                    version,
                    changes.appendAll(newEvents)));
  }

  public Aggregate<C, E, Er, S> markAsCommitted() {
    return new Aggregate<>(
        id, aggregateLogic, state, version.incrementBy(changes.length()), List.empty());
  }

  public Aggregate<C, E, Er, S> reconstitute(List<E> events) {
    return new Aggregate<>(
        id,
        aggregateLogic,
        events.foldLeft(state, State::apply),
        version.incrementBy(events.length()),
        List.empty());
  }

  public Aggregate<C, E, Er, S> reconstitute(S state, Version version) {
    return new Aggregate<>(id, aggregateLogic, state, version, List.empty());
  }
}
