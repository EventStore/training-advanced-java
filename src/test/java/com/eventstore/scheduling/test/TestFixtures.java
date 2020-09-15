package com.eventstore.scheduling.test;

import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorDayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.DoctorId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.PatientId;

import java.time.*;
import java.util.UUID;

public class TestFixtures {
  public static final LocalDate today = LocalDate.now(Clock.systemDefaultZone());
  public static final LocalTime tenAm = LocalTime.of(10, 0);
  public static final LocalDateTime tenAmToday = LocalDateTime.of(today, tenAm);
  public static final Duration tenMinutes = Duration.ofMinutes(10);
  public static final DoctorId doctorId = new DoctorId(randomString());
  public static final PatientId patientId = new PatientId(randomString());
  public static final DayId dayId = new DayId(doctorId, today);
  public static final SlotId slotId = new SlotId(randomString());
  public static final DoctorDayId doctorDayId = new DoctorDayId(dayId);

  public static String randomString() {
    return UUID.randomUUID().toString().replace("-", "").substring(8);
  }
}
