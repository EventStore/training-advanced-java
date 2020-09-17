package com.eventstore.scheduling.domain.doctorday;

import com.eventstore.scheduling.domain.doctorday.event.SlotScheduled;
import io.vavr.collection.List;
import lombok.Data;
import lombok.NonNull;

import java.time.Duration;
import java.time.LocalTime;

@Data
public class Slots {
    private @NonNull List<Slot> slots;

    public void add(SlotScheduled slotScheduled) {
        slots = slots.append(
                new Slot(
                        slotScheduled.getSlotId(),
                        slotScheduled.getStartDateTime().toLocalTime(),
                        slotScheduled.getDuration(),
                        false));
    }

    public void remove(SlotId slotId) {
        slots = slots.filter((slot) -> (!slot.getSlotId().equals(slotId)));
    }

    public void markAsBooked(SlotId slotId) {
        slots = slots.map(
                (slot) -> {
                    if (slot.getSlotId().equals(slotId)) {
                        return new Slot(slot.getSlotId(), slot.getStartTime(), slot.getDuration(), true);
                    }
                    return slot;
                });
    }

    public void markAsAvailable(SlotId slotId) {
        slots = slots.map(
                (slot) -> {
                    if (slot.getSlotId().equals(slotId)) {
                        return new Slot(slot.getSlotId(), slot.getStartTime(), slot.getDuration(), false);
                    }
                    return slot;
                });
    }

    public boolean overlapsWith(LocalTime startTime, Duration duration) {
        return slots.exists((slot) -> (slot.overlapsWith(startTime, duration)));
    }

    public SlotStatus getState(SlotId slotId) {
        return slots.find(slot -> slot.getSlotId().equals(slotId)).map(slot -> {
            if (slot.getBooked()) {
                return SlotStatus.Booked;
            } else {
                return SlotStatus.Available;
            }
        }).getOrElse(SlotStatus.NotScheduled);
    }

    public boolean hasBookedSlot(SlotId slotId) {
        return slots.exists(slot -> slot.getSlotId().equals(slotId) && slot.getBooked());
    }

    public List<Slot> getBookedSlots() {
        return slots.filter(Slot::getBooked);
    }
}
