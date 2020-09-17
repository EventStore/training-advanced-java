package com.eventstore.scheduling.eventsourcing;

import lombok.Data;
import lombok.NonNull;

@Data
public class Version {
  public static Version fresh = new Version(-1L);
  private final @NonNull Long value;

  public Version incrementBy(int length) {
    return new Version(value + length);
  }
}
