package com.eventstore.scheduling.application.http;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorId;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.ScheduleDay;
import com.eventstore.scheduling.domain.writemodel.doctorday.command.ScheduleSlot;
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
    return new ScheduleDay(
        new DoctorId(doctorId),
        date,
        io.vavr.collection.List.ofAll(slots)
            .map(slot -> new ScheduleSlot(slot.startTime, Duration.parse(slot.duration))));
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
