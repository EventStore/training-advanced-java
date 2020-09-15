package com.eventstore.scheduling.test;

import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.service.RandomIdGenerator;
import io.vavr.collection.List;
import lombok.val;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static io.vavr.API.None;
import static io.vavr.API.Some;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class EventHandlerTest {
  protected final IdGenerator idGenerator = new ReplayableIdGenerator();
  protected final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
  protected final CommandStore commandStore = new InMemoryCommandStore(enableWonkyIoMonkey());
  private final RandomIdGenerator randomIdGenerator = new RandomIdGenerator();
  protected EventMetadata lastEventMetadata = null;

  protected boolean enableAtLeastOnceMonkey() {
    return false;
  }

  protected boolean enableWonkyIoMonkey() {
    return false;
  }

  protected abstract MessageHandler<EventMetadata> handler();

  protected EventMetadata generateMetadata(boolean replayed) {
    if (replayed) {
      return new EventMetadata(
          CorrelationId.create(randomIdGenerator),
          CausationId.create(randomIdGenerator),
          Some("replay_id"));
    } else {
      return new EventMetadata(
          CorrelationId.create(randomIdGenerator), CausationId.create(randomIdGenerator), None());
    }
  }

  protected void given(List<Object> events) {
    handleEvents(events, true);
    ((ReplayableIdGenerator) idGenerator).reset();
  }

  private void handleEvents(List<Object> events, boolean replayed) {
    List<MessageEnvelope<EventMetadata>> eventsInEnvelopes = events
        .zipWithIndex()
            .map((eventWithIndex) -> {
              EventMetadata eventMetadata = generateMetadata(replayed);
              return new MessageEnvelope<>(
                              eventWithIndex._1,
                              eventMetadata,
                              randomIdGenerator.nextUuid(),
                              clock.instant(),
                              new Version(eventWithIndex._2.longValue()),
                              Some(new Version(eventWithIndex._2.longValue())));

            });

      eventsInEnvelopes.forEach(event -> {
          handler().handle(event);
          if (enableAtLeastOnceMonkey()) {
              // Let's repeat every event
              handler().handle(event);
          }
      });

      // And repeat whole sequence except last event
      if (enableAtLeastOnceMonkey()) {
          eventsInEnvelopes.dropRight(1).forEach(event -> {
              handler().handle(event);
          });
      }
  }

  protected void then(Object actual, Object expected) {
    assertEquals(expected, actual);
  }

  protected void then(List<CommandEnvelope> expectedCommands) {
    assertEquals(expectedCommands, ((InMemoryCommandStore) commandStore).get());
  }

  protected void when(Object event) {
    ((ReplayableIdGenerator) idGenerator).reset();
    handleEvents(List.of(event), false);
    ((ReplayableIdGenerator) idGenerator).replay();
  }
}
