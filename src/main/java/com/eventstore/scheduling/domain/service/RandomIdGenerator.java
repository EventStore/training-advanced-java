package com.eventstore.scheduling.domain.service;

import java.util.UUID;

public class RandomIdGenerator implements IdGenerator {
  @Override
  public UUID nextUuid() {
    return UUID.randomUUID();
  }
}
