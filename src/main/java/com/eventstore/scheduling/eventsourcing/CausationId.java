package com.eventstore.scheduling.eventsourcing;

import com.eventstore.scheduling.domain.service.IdGenerator;
import lombok.Data;
import lombok.NonNull;

@Data
public class CausationId {
  private final @NonNull String value;

  public static CausationId create(IdGenerator idGenerator) {
    return new CausationId(idGenerator.nextUuid().toString());
  }
}
