package com.eventstore.scheduling.test;

import com.eventstore.scheduling.eventsourcing.CommandEnvelope;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CommandStore;
import io.vavr.collection.List;

public class InMemoryCommandStore implements CommandStore {
  private final boolean monkeyEnabled;
  private List<CommandEnvelope> commands = List.empty();
  private boolean throwOnNextCall = true;

  public InMemoryCommandStore(boolean monkeyEnabled) {
    this.monkeyEnabled = monkeyEnabled;
  }

  @Override
  public void send(Object command, CommandMetadata metadata) {
    if (monkeyEnabled) {
      if (throwOnNextCall) {
        throwOnNextCall = false;
        throw new RuntimeException("Wonky IO monkey!");
      } else {
        throwOnNextCall = true;
        commands = commands.append(new CommandEnvelope(command, metadata));
      }
    } else {
      commands = commands.append(new CommandEnvelope(command, metadata));
    }
  }

    @Override
    public void start() {

    }

    public List<CommandEnvelope> get() {
    return commands;
  }
}
