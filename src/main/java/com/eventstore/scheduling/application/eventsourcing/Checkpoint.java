package com.eventstore.scheduling.application.eventsourcing;

import lombok.Data;
import lombok.NonNull;

@Data
public class Checkpoint {
  private final @NonNull Long value;
}
