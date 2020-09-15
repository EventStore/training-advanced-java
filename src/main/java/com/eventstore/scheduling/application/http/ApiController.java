package com.eventstore.scheduling.application.http;

import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlotsRepository;
import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.Command;
import com.eventstore.scheduling.domain.writemodel.doctorday.error.Error;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.CalendarDayStarted;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.Event;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.State;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static io.vavr.API.None;

@RestController
@RequestMapping("/api")
public class ApiController {

  @Autowired private AvailableSlotsRepository availableSlotsRepository;

  @Autowired private CommandHandler<Command, Event, Error, State> handler;
  @Autowired private EventStoreClient<EventMetadata> eventStoreClient;

  @GetMapping(value = "/slots/today/available")
  public List<GetAvailableSlot> getAvailableSlotsToday() {
    return availableSlotsRepository
        .getAvailableSlotsOn(LocalDate.now())
        .map(GetAvailableSlot::fromDomain)
        .asJava();
  }

  @GetMapping(value = "/slots/{date}/available")
  public List<GetAvailableSlot> getAvailableSlotsOnADate(
      @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return availableSlotsRepository
        .getAvailableSlotsOn(date)
        .map(GetAvailableSlot::fromDomain)
        .asJava();
  }

  @PostMapping(value = "/doctor/schedule")
  public ResponseEntity<?> scheduleDay(
      @RequestBody PostScheduleDay scheduleDay,
      @RequestHeader("X-CorrelationId") String correlationId,
      @RequestHeader("X-CausationId") String causationId) {

    val command = scheduleDay.toCommand();
    val aggregateId = scheduleDay.toAggregateId();
    val result = handler.handle(command, getMetadata(correlationId, causationId, aggregateId));

    if (result.isRight()) {
      return ResponseEntity.status(HttpStatus.CREATED)
          .header("Location", "/api/slots/" + command.getDate() + "/available")
          .body(null);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getLeft().toString());
    }
  }

  @PostMapping(value = "/slots/{dayId}/book")
  public ResponseEntity<?> bookSlot(
      @RequestBody PostBookSlot bookSlot,
      @PathVariable("dayId") String dayId,
      @RequestHeader("X-CorrelationId") String correlationId,
      @RequestHeader("X-CausationId") String causationId) {

    val command = bookSlot.toCommand();
    val aggregateId = new DoctorDayId(new DayId(dayId));
    val result = handler.handle(command, getMetadata(correlationId, causationId, aggregateId));

    if (result.isRight()) {
      return ResponseEntity.status(HttpStatus.OK).body(null);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getLeft().toString());
    }
  }

  private CommandMetadata getMetadata(
      @RequestHeader("X-CorrelationId") String correlationId,
      @RequestHeader("X-CausationId") String causationId,
      DoctorDayId aggregateId) {
    return new CommandMetadata(
        new CorrelationId(correlationId), new CausationId(causationId), aggregateId);
  }

  @PostMapping(value = "/slots/{dayId}/cancel-booking")
  public ResponseEntity<?> cancelSlotBooking(
      @RequestBody PostCancelSlotBooking cancelSlotBooking,
      @PathVariable("dayId") String dayId,
      @RequestHeader("X-CorrelationId") String correlationId,
      @RequestHeader("X-CausationId") String causationId) {

    val command = cancelSlotBooking.toCommand();
    val aggregateId = new DoctorDayId(new DayId(dayId));
    val result = handler.handle(command, getMetadata(correlationId, causationId, aggregateId));

    if (result.isRight()) {
      return ResponseEntity.status(HttpStatus.OK).body(null);
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getLeft().toString());
    }
  }

  @PostMapping(value = "/calendar/{date}/day-started")
  public ResponseEntity<?> calendarDayStarted(
      @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestHeader("X-CorrelationId") String correlationId,
      @RequestHeader("X-CausationId") String causationId) {

    val eventMetadata =
        new EventMetadata(new CorrelationId(correlationId), new CausationId(causationId), None());

    eventStoreClient.appendToStream(
        "doctorday-time-events",
        io.vavr.collection.List.of(new CalendarDayStarted(date)),
        eventMetadata);

    return ResponseEntity.status(HttpStatus.OK).body(null);
  }
}
