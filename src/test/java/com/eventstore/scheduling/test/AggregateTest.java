package com.eventstore.scheduling.test;

import com.eventstore.scheduling.application.eventsourcing.Aggregate;
import com.eventstore.scheduling.application.eventsourcing.Version;
import com.eventstore.scheduling.domain.writemodel.AggregateId;
import com.eventstore.scheduling.domain.writemodel.AggregateLogic;
import com.eventstore.scheduling.domain.writemodel.State;
import io.vavr.collection.List;
import io.vavr.control.Either;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AggregateTest<C, E, Er, S extends State<S, E>> {
  protected final ReplayableIdGenerator idGenerator = new ReplayableIdGenerator();
  protected Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
  private Aggregate<C, E, Er, S> aggregate = null;

  private Either<Er, Aggregate<C, E, Er, S>> result = null;

  protected abstract AggregateLogic<C, E, Er, S> aggregateLogic();

  protected void given(List<E> events) {
    aggregate = aggregate.reconstitute(events);
  }

  protected void when(C command) {
    idGenerator.reset();
    result = aggregate.handle(command);
    idGenerator.replay();
  }

  protected void then(List<E> events) {
    if (result.isLeft()) {
      throw new RuntimeException(
          "Expected " + events.toString() + " but got " + result.getLeft().toString());
    } else {
      result.forEach(
          (aggregate) -> {
            assertEquals(aggregate.getChanges(), events);

            val version = aggregate.getVersion();
            val committed = aggregate.markAsCommitted();
            assertEquals(
                committed.getVersion(), version.incrementBy(aggregate.getChanges().length()));
          });
    }
  }

  protected void then(Er error) {
    if (result.isLeft()) {
      assertEquals(result.getLeft(), error);
    } else {
      result.forEach(
          (aggregate) -> {
            throw new RuntimeException(
                "Expected " + error.toString() + " but got " + aggregate.getChanges().toString());
          });
    }
  }

  @BeforeEach
  void beforeEach() {
    aggregate =
        new Aggregate(
            new AggregateId(randomString(), "test"),
            aggregateLogic(),
            aggregateLogic().initialState(),
            Version.fresh,
            List.empty());
    result = null;
    idGenerator.reset();
  }

  protected String randomString() {
    return UUID.randomUUID().toString();
  }

  //
  //    protected String id;
  //    protected A aggregate;
  //
  //    private Try<List<? extends Event>> result;
  //
  //    protected String randomString() {
  //        return UUID.randomUUID().toString();
  //    }
  //
  //    @BeforeEach
  //    void beforeEach() {
  //        id = randomString();
  //        aggregate = newInstance();
  //        result = null;
  //        uuidGenerator.reset();
  //    }
  //
  //    @Test
  //    void canBeInitialised() {
  //        assertEquals(aggregate.getId(), id);
  //        assertEquals(aggregate.getVersion(), -1L);
  //    }
  //
  //    protected void given(Event... events) {
  //        aggregate.reconstitute(List.of(events));
  //    }
  //
  //
  //    protected void then(Event event) {
  //        List<? extends Event> changes = result.get();
  //        assertEquals(changes, List.of(event));
  //
  //        val version = aggregate.getVersion();
  //        aggregate.markAsCommitted();
  //        assertEquals(aggregate.getVersion(), version + changes.length());
  //    }
  //
  //    protected void then(Error error) {
  //        assertEquals(error, result.failed().get());
  //    }
  //
  //    protected abstract A newInstance();
}
