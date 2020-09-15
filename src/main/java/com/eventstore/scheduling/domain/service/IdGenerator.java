package com.eventstore.scheduling.domain.service;

import java.util.UUID;

public interface IdGenerator {
  UUID nextUuid();
}
