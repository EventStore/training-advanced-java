package com.eventstore.scheduling.infrastructure.projections;

public interface Subscription {
    void project(Object event);
}
