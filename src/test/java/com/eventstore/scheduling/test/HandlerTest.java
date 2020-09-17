package com.eventstore.scheduling.test;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
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
    events.forEach(event -> {
      handler().handle(event);

      if (enableAtLeastOnceMonkey()) {
        handler().handle(event);
      }
    });

    if (enableAtLeastOnceGorilla()) {
      events.dropRight(1).forEach(event -> {
        handler().handle(event);
      });
    }
  }

  protected void then(Object actual, Object expected) {
    assertEquals(expected, actual);
  }
}
