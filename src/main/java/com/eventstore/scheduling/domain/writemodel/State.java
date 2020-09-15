package com.eventstore.scheduling.domain.writemodel;

public abstract class State<S extends State<S, E>, E> {
  public abstract S apply(E event);
}
