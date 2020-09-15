package com.eventstore.scheduling.test;

import com.eventstore.dbclient.Subscription;
import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.writemodel.State;
import io.vavr.collection.List;

import java.util.concurrent.CompletableFuture;

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
  public <C, E, Er, S extends State<S, E>> CompletableFuture<Subscription> subscribe(
      CommandHandler<C, E, Er, S> commandHandler, CheckpointStore checkpointStore) {
    return null;
  }

  public List<CommandEnvelope> get() {
    return commands;
  }
}
