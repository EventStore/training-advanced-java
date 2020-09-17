package com.eventstore.scheduling.test;

import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.eventsourcing.*;
import com.eventstore.scheduling.infrastructure.commands.CommandHandler;
import com.eventstore.scheduling.infrastructure.commands.CommandHandlerMap;
import com.eventstore.scheduling.infrastructure.commands.Dispatcher;
import io.vavr.collection.List;
import lombok.SneakyThrows;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AggregateTest<A extends AggregateRoot, T> {
  private Dispatcher dispatcher;
  protected final T repository;
  private final AggregateRoot aggregate;
  private Throwable exception;
  protected ReplayableIdGenerator idGenerator = new ReplayableIdGenerator();
  protected RandomIdGenerator randomIdGenerator = new RandomIdGenerator();

  protected abstract AggregateRoot aggregateInstance();
  protected abstract T repositoryInstance(AggregateStore aggregateStore);

  public AggregateTest() {
    aggregate = aggregateInstance();
    repository = repositoryInstance(new FakeAggregateStore(aggregate));
  }

  public <CH extends CommandHandler> void registerHandlers(CH commandHandler) {
    dispatcher = new Dispatcher(new CommandHandlerMap(commandHandler));
  }

  protected void given(Object... events) {
    exception = null;
    aggregate.load(List.of(events));
  }

  protected void when(Object command) {
    try {
      aggregate.clearChanges();
      idGenerator.replay();
      dispatcher.dispatch(command, new CommandMetadata(CorrelationId.create(randomIdGenerator), CausationId.create(randomIdGenerator)));
    } catch (Throwable e) {
      exception = e;
    }
  }

  @SneakyThrows
  protected void then(Consumer<List<Object>> events) {
    if (exception != null) {
      throw exception;
    }

    events.accept(aggregate.getChanges());
    idGenerator.reset();
  }
  @SneakyThrows
  protected void then(List<Object> events) {
    assertEquals(events, aggregate.getChanges());
    idGenerator.reset();
  }

  protected <E extends Throwable> void then(Class<E> clazz) {
    assertEquals(clazz, exception.getClass());
    idGenerator.reset();
  }
}
