package com.eventstore.scheduling.controllers;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.event.CalendarDayStarted;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlotsRepository;
import com.eventstore.scheduling.eventsourcing.CausationId;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CorrelationId;
import com.eventstore.scheduling.eventsourcing.EventStore;
import com.eventstore.scheduling.infrastructure.commands.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {
    @Autowired
    private Dispatcher dispatcher;

    @Autowired
    private AvailableSlotsRepository availableSlotsRepository;

    @Autowired
    private EventStore eventStore;

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

        dispatcher.dispatch(scheduleDay.toCommand(), CommandMetadata.of(correlationId, causationId));

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/slots/%s/available".formatted(scheduleDay.toCommand().date()))
                .body(null);
    }

    @PostMapping(value = "/slots/{dayId}/book")
    public ResponseEntity<?> bookSlot(
            @RequestBody PostBookSlot bookSlot,
            @PathVariable("dayId") String dayId,
            @RequestHeader("X-CorrelationId") String correlationId,
            @RequestHeader("X-CausationId") String causationId) {

        dispatcher.dispatch(bookSlot.toCommand(new DayId(dayId)), CommandMetadata.of(correlationId, causationId));

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PostMapping(value = "/slots/{dayId}/cancel-booking")
    public ResponseEntity<?> cancelSlotBooking(
            @RequestBody PostCancelSlotBooking cancelSlotBooking,
            @PathVariable("dayId") String dayId,
            @RequestHeader("X-CorrelationId") String correlationId,
            @RequestHeader("X-CausationId") String causationId) {

        dispatcher.dispatch(cancelSlotBooking.toCommand(new DayId(dayId)), CommandMetadata.of(correlationId, causationId));

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PostMapping(value = "/calendar/{date}/day-started")
    public ResponseEntity<?> calendarDayStarted(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestHeader("X-CorrelationId") String correlationId,
            @RequestHeader("X-CausationId") String causationId) {

        eventStore.appendEvents(
                "doctorday-time-events",
                new CommandMetadata(new CorrelationId(correlationId), new CausationId(causationId)),
                io.vavr.collection.List.of(new CalendarDayStarted(date))
                );

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
