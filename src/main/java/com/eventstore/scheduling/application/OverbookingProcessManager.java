package com.eventstore.scheduling.application;

import com.eventstore.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository;
import com.eventstore.scheduling.domain.readmodel.bookedslots.Slot;
import com.eventstore.scheduling.domain.service.IdGenerator;
import com.eventstore.scheduling.domain.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.doctorday.command.CancelSlotBooking;
import com.eventstore.scheduling.domain.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import com.eventstore.scheduling.eventsourcing.CausationId;
import com.eventstore.scheduling.eventsourcing.CommandMetadata;
import com.eventstore.scheduling.eventsourcing.CommandStore;
import com.eventstore.scheduling.eventsourcing.CorrelationId;
import com.eventstore.scheduling.infrastructure.projections.EventHandler;
import lombok.val;

public class OverbookingProcessManager extends EventHandler {

  public OverbookingProcessManager(
      BookedSlotsRepository repository,
      CommandStore commandStore,
      int bookingLimitPerPatient,
      IdGenerator idGenerator) {

    when(SlotScheduled.class, slotScheduled -> repository.addSlot(
            new Slot(
                    slotScheduled._1.getSlotId(),
                    slotScheduled._1.getDayId(),
                    slotScheduled._1.getStartDateTime().getMonth())));

    when(SlotBooked.class, slotBooked -> {
      repository.markSlotAsBooked(slotBooked._1.getSlotId(), slotBooked._1.getPatientId());

      val slot = repository.getSlot(slotBooked._1.getSlotId());
      val count = repository.countByPatientAndMonth(slotBooked._1.getPatientId(), slot.getMonth());

      if (count > bookingLimitPerPatient) {
        commandStore.send(
                new CancelSlotBooking(slot.getDayId(), slot.getSlotId(), "Overbooking"),
                new CommandMetadata(
                        slotBooked._2.getCorrelationId(),
                        CausationId.create(idGenerator)));
      }
    });

    when(SlotBookingCancelled.class, slotBookedCancelled -> repository.markSlotAsAvailable(slotBookedCancelled._1.getSlotId()));
  }
}
