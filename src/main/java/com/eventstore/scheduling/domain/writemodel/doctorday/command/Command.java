package com.eventstore.scheduling.domain.writemodel.doctorday.command;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.State;
import io.vavr.collection.List;
import io.vavr.control.Either;

public interface Command {
  Either<Error, List<Event>> apply(State state, IdGenerator idGenerator);
}
