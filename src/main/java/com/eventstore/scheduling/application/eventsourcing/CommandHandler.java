package com.eventstore.scheduling.application.eventsourcing;

import com.eventstore.scheduling.domain.writemodel.AggregateLogic;
import com.eventstore.scheduling.domain.writemodel.State;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.val;

import static io.vavr.API.None;

public class CommandHandler<C, E, Er, S extends State<S, E>> {
  private final AggregateLogic<C, E, Er, S> aggregateLogic;
  private final AggregateStore aggregateStore;
  private final SnapshotStore snapshotStore;
  private final Option<Long> snapshotThreshold;

  public CommandHandler(
      AggregateLogic<C, E, Er, S> aggregateLogic,
      AggregateStore aggregateStore,
      SnapshotStore snapshotStore,
      Option<Long> snapshotThreshold) {
    this.aggregateLogic = aggregateLogic;
    this.aggregateStore = aggregateStore;
    this.snapshotStore = snapshotStore;
    this.snapshotThreshold = snapshotThreshold;
  }

  public Either<Er, Aggregate<C, E, Er, S>> handle(C command, CommandMetadata metadata) {
    val aggregate = Aggregate.instance(metadata.getAggregateId(), aggregateLogic);
    val metatdata =
        new EventMetadata(metadata.getCorrelationId(), metadata.getCausationId(), None());

    Aggregate<C, E, Er, S> withSnapshot =
        snapshotStore
            .read(aggregate.getId())
            .map(
                envelope ->
                    aggregate.reconstitute(
                        (S) envelope.getSnapshot(), envelope.getMetadata().getVersion()))
            .getOrElse(aggregate);

    val withEvents = Try.of(() -> aggregateStore.reconsititute(withSnapshot)).getOrElse(aggregate);

    return withEvents
        .handle(command)
        .map(
            handled -> {
              val committed = aggregateStore.commit(handled, metatdata);
              snapshotThreshold.forEach(
                  threshold -> {
                    if ((committed.getVersion().getValue() - withSnapshot.getVersion().getValue()) >= threshold) {
                      snapshotStore.write(
                          committed.getId(),
                          committed.getState(),
                          new SnapshotMetadata(
                              metatdata.getCorrelationId(),
                              metadata.getCausationId(),
                              committed.getVersion()));
                    }
                  });
              return committed;
            });
  }
}
