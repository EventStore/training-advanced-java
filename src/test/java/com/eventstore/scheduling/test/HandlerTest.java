package com.eventstore.scheduling.test;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.eventsourcing.CausationId;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CorrelationId;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import io.vavr.Tuple2;
import io.vavr.collection.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class HandlerTest {
  protected IdGenerator idGenerator = new RandomIdGenerator();

  protected abstract EventHandler handler();

  protected boolean enableAtLeastOnceMonkey() {
    return false;
  }
  protected boolean enableAtLeastOnceGorilla() {
    return false;
  }

  protected void given(List<Object> events) {
    CommandMetadata commandMetadata = new CommandMetadata(CorrelationId.create(idGenerator), CausationId.create(idGenerator));
    events.forEach(event -> {
      handler().handle(new Tuple2(event, commandMetadata));

      if (enableAtLeastOnceMonkey()) {
        handler().handle(new Tuple2(event, commandMetadata));
      }
    });

    if (enableAtLeastOnceGorilla()) {
      events.dropRight(1).forEach(event -> {
        handler().handle(new Tuple2(event, commandMetadata));
      });
    }
  }

  protected void then(Object actual, Object expected) {
    assertEquals(expected, actual);
  }
}
