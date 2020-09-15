package com.eventstore.scheduling.domain.writemodel;

import io.vavr.collection.List;
import io.vavr.control.Either;

public abstract class AggregateLogic<C, E, Er, S extends State<S, E>> {
  public abstract Either<Er, List<E>> apply(S state, C command);

  public abstract S initialState();
}
