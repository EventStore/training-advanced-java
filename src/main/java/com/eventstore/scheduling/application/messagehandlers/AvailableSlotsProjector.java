package com.eventstore.scheduling.application.messagehandlers;

import com.eventstore.scheduling.application.eventsourcing.EventMetadata;
import com.eventstore.scheduling.application.eventsourcing.MessageHandler;
import com.eventstore.scheduling.application.eventsourcing.Version;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlotsRepository;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBooked;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotBookingCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotCancelled;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotScheduled;
import io.vavr.control.Option;
import lombok.val;

import java.time.Instant;
import java.util.UUID;

public class AvailableSlotsProjector implements MessageHandler<EventMetadata> {
  private final AvailableSlotsRepository repository;

  public AvailableSlotsProjector(AvailableSlotsRepository repository) {
    super();
    this.repository = repository;
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
          new AvailableSlot(
              slotScheduled.getDayId(),
              slotScheduled.getSlotId(),
              slotScheduled.getStartDateTime().toLocalDate(),
              slotScheduled.getStartDateTime().toLocalTime(),
              slotScheduled.getDuration().toString()));
    }
    if (message instanceof SlotBooked) {
      repository.hideSlot(((SlotBooked) message).getSlotId());
    }
    if (message instanceof SlotBookingCancelled) {
      repository.showSlot(((SlotBookingCancelled) message).getSlotId());
    }
    if (message instanceof SlotCancelled) {
      repository.deleteSlot(((SlotCancelled) message).getSlotId());
    }
  }
}
