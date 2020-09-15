package com.eventstore.scheduling.application.messagehandlers;

import com.eventstore.scheduling.application.eventsourcing.*;
import com.eventstore.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository;
import com.eventstore.scheduling.domain.readmodel.bookedslots.Slot;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.CancelSlotBooking;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotScheduled;
import io.vavr.control.Option;
import lombok.val;

import java.time.Instant;
import java.util.UUID;

public class OverbookingProcessManager implements MessageHandler<EventMetadata> {
  private final BookedSlotsRepository repository;
  private final CommandStore commandStore;
  private final int bookingLimitPerPatient;
  private final IdGenerator idGenerator;

  public OverbookingProcessManager(
      BookedSlotsRepository repository,
      CommandStore commandStore,
      int bookingLimitPerPatient,
      IdGenerator idGenerator) {
    this.repository = repository;
    this.commandStore = commandStore;
    this.bookingLimitPerPatient = bookingLimitPerPatient;
    this.idGenerator = idGenerator;
  }

  @Override
  public void handle(
      Object message,
      EventMetadata metadata,
      UUID messageId,
      Instant occurredAt,
      Version position,
      Option<Version> streamPosition) {
    if (message instanceof SlotScheduled) {
      val slotScheduled = (SlotScheduled) message;
      repository.addSlot(
          new Slot(
              slotScheduled.getSlotId(),
              slotScheduled.getDayId(),
              slotScheduled.getStartDateTime().getMonth()));
    }
    if (message instanceof SlotBooked) {
      val slotBooked = (SlotBooked) message;
      repository.markSlotAsBooked(slotBooked.getSlotId(), slotBooked.getPatientId());

      val slot = repository.getSlot(slotBooked.getSlotId());
      val count = repository.countByPatientAndMonth(slotBooked.getPatientId(), slot.getMonth());

      if (count > bookingLimitPerPatient) {
        commandStore.send(
            new CancelSlotBooking(slot.getSlotId(), "Overbooking"),
            new CommandMetadata(
                metadata.getCorrelationId(),
                CausationId.create(idGenerator),
                new DoctorDayId(slot.getDayId())));
      }
    }
    if (message instanceof SlotBookingCancelled) {
      val slotBookedCancelled = (SlotBookingCancelled) message;
      repository.markSlotAsAvailable(slotBookedCancelled.getSlotId());
    }
  }
}
