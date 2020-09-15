package com.eventstore.scheduling.domain.writemodel;

import lombok.Data;
import lombok.NonNull;

@Data
public class AggregateId {
  private final @NonNull String value;
  private final @NonNull String type;

  public String toString() {
    return type + "-" + value;
  }
}
