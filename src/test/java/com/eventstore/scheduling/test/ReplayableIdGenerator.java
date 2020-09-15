package com.eventstore.scheduling.test;

import com.eventstore.scheduling.domain.service.IdGenerator;
import io.vavr.collection.List;

import java.util.UUID;

public class ReplayableIdGenerator implements IdGenerator {
  private boolean replayMode = false;
  private List<UUID> ids = List.empty();

  @Override
  public UUID nextUuid() {
    if (replayMode) {
      if (ids.isEmpty())
        throw new RuntimeException("Replayable UUID generator doesn't have any ids left to replay");
      UUID next = ids.head();
      ids = ids.tail();
      return next;
    } else {
      UUID id = UUID.randomUUID();
      ids = ids.append(id);
      return id;
    }
  }

  public void replay() {
    replayMode = true;
  }

  public void reset() {
    replayMode = false;
    ids = List.empty();
  }
}
