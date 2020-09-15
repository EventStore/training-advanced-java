package com.eventstore.scheduling.domain.writemodel.doctorday;

import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.writemodel.AggregateLogic;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.Command;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.State;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.Unscheduled;
import io.vavr.collection.List;
import io.vavr.control.Either;

public class DoctorDayLogic extends AggregateLogic<Command, Event, Error, State> {
  private final IdGenerator idGenerator;

  public DoctorDayLogic(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  @Override
  public Either<Error, List<Event>> apply(State state, Command command) {
    return command.apply(state, idGenerator);
  }

  @Override
  public State initialState() {
    return new Unscheduled();
  }
}
