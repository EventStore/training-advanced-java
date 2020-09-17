package com.eventstore.scheduling.eventsourcing;

import lombok.Data;
import lombok.NonNull;

@Data
public class SubscriptionId {
  private final @NonNull String value;
}
