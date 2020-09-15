package com.eventstore.scheduling.domain.writemodel.doctorday.state;

import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.event.SlotScheduled;
import io.vavr.collection.List;
import lombok.Data;
import lombok.NonNull;

@Data
public class Slots {
  private final @NonNull List<Slot> slots;

  public Slots add(SlotScheduled slotScheduled) {
    return new Slots(
        slots.append(
            new Slot(
                slotScheduled.getSlotId(),
                slotScheduled.getStartDateTime().toLocalTime(),
                slotScheduled.getDuration(),
                false)));
  }

  public Slots remove(SlotId slotId) {
    return new Slots(slots.filter((slot) -> (!slot.getSlotId().equals(slotId))));
  }

  public Slots markAsBooked(SlotId slotId) {
    return new Slots(
        slots.map(
            (slot) -> {
              if (slot.getSlotId().equals(slotId)) {
                return new Slot(slot.getSlotId(), slot.getStartTime(), slot.getDuration(), true);
              }
              return slot;
            }));
  }

  public Slots markAsAvailable(SlotId slotId) {
    return new Slots(
        slots.map(
            (slot) -> {
              if (slot.getSlotId().equals(slotId)) {
                return new Slot(slot.getSlotId(), slot.getStartTime(), slot.getDuration(), false);
              }
              return slot;
            }));
  }
}
