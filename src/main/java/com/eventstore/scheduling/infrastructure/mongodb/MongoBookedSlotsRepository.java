package com.eventstore.scheduling.infrastructure.mongodb;

import com.eventstore.scheduling.domain.readmodel.bookedslots.BookedSlotsRepository;
import com.eventstore.scheduling.domain.readmodel.bookedslots.Slot;
import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.eventstore.scheduling.domain.writemodel.doctorday.SlotId;
import com.eventstore.scheduling.domain.writemodel.doctorday.state.PatientId;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.vavr.collection.List;
import lombok.*;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import java.time.Month;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoBookedSlotsRepository implements BookedSlotsRepository {
  private final MongoCollection<SlotDateRow> slotMonths;

  private final MongoCollection<PatientSlotRow> patientSlots;

  public MongoBookedSlotsRepository(MongoDatabase database) {
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(
                new CustomCodecProvider(), PojoCodecProvider.builder().automatic(true).build()));
    slotMonths =
        database
            .withCodecRegistry(pojoCodecRegistry)
            .getCollection("slot_months", SlotDateRow.class);
    patientSlots =
        database
            .withCodecRegistry(pojoCodecRegistry)
            .getCollection("patient_slots", PatientSlotRow.class);
  }

  @Override
  public int countByPatientAndMonth(PatientId patientId, Month month) {
    return (int)
        patientSlots.countDocuments(
            and(eq("patientId", patientId.getValue()), eq("monthNumber", month.getValue())));
  }

  @Override
  public void addSlot(Slot slot) {
    slotMonths.insertOne(
        new SlotDateRow(
            new ObjectId(),
            slot.getSlotId().getValue(),
            slot.getDayId().getValue(),
            slot.getMonth().getValue()));
  }

  @Override
  public void markSlotAsBooked(SlotId slotId, PatientId patientId) {
    val maybeSlot = List.ofAll(slotMonths.find(eq("slotId", slotId.getValue()))).headOption();
    maybeSlot.forEach(
        (slotDateRow) -> {
          patientSlots.insertOne(
              new PatientSlotRow(
                  new ObjectId(),
                  patientId.getValue(),
                  slotDateRow.getSlotId(),
                  slotDateRow.getMonthNumber()));
        });
  }

  @Override
  public void markSlotAsAvailable(SlotId slotId) {
    patientSlots.deleteOne(eq("slotId", slotId.getValue()));
  }

  @Override
  public Slot getSlot(SlotId slotId) {
    return List.ofAll(slotMonths.find(eq("slotId", slotId.getValue())))
        .headOption()
        .map(
            (slotMonthRow) ->
                new Slot(
                    new SlotId(slotMonthRow.getSlotId()),
                    new DayId(slotMonthRow.getDayId()),
                    Month.of(slotMonthRow.getMonthNumber())))
        .get();
  }

  @EqualsAndHashCode
  @ToString
  @AllArgsConstructor
  @Getter
  @Setter
  @NoArgsConstructor
  public static class SlotDateRow {
    private ObjectId id;
    private String slotId;
    private String dayId;
    private int monthNumber;
  }

  @EqualsAndHashCode
  @ToString
  @AllArgsConstructor
  @Getter
  @Setter
  @NoArgsConstructor
  public static class PatientSlotRow {
    private ObjectId id;
    private String patientId;
    private String slotId;
    private int monthNumber;
  }
}
