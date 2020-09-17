package com.eventstore.scheduling.infrastructure.eventstore;

import lombok.Data;
import lombok.NonNull;

@Data
public class Checkpoint {
  private final @NonNull Long value;
}
