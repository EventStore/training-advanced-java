package com.eventstore.scheduling.application.eventsourcing;

import lombok.Data;
import lombok.NonNull;

@Data
public class CommandEnvelope {
  private final @NonNull Object command;
  private final @NonNull CommandMetadata metadata;
}
