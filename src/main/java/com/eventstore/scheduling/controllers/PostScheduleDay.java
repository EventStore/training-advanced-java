package com.eventstore.scheduling.controllers;

import com.eventstore.scheduling.domain.doctorday.DayId;
import com.eventstore.scheduling.domain.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.doctorday.DoctorId;
import com.eventstore.scheduling.domain.doctorday.command.ScheduleDay;
import com.eventstore.scheduling.domain.doctorday.command.ScheduleSlot;
import lombok.Data;
import lombok.NonNull;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class PostScheduleDay {
    public @NonNull LocalDate date;
    public @NonNull String doctorId;
    public @NonNull List<PostSlot> slots;

    public ScheduleDay toCommand() {
        DoctorId doctorId = new DoctorId(this.doctorId);
        return new ScheduleDay(
                doctorId,
                date,
                io.vavr.collection.List.ofAll(slots)
                        .map(slot -> new ScheduleSlot(new DayId(doctorId, date), slot.startTime, Duration.parse(slot.duration))));
    }

    public DoctorDayId toAggregateId() {
        return new DoctorDayId(new DayId(new DoctorId(doctorId), date));
    }

    @Data
    public static class PostSlot {
        public @NonNull String duration;
        public @NonNull LocalTime startTime;
    }
}
