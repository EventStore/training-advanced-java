package com.eventstore.scheduling.infrastructure.projections;


public class DbProjector implements Subscription {
    private final EventHandler projection;

    public DbProjector(EventHandler projection) {
        this.projection = projection;
    }

    @Override
    public void project(Object event) {
        if (projection.canHandle(event)) {
            projection.handle(event);
        }
    }
}

