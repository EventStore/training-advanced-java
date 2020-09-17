package com.eventstore.scheduling.infrastructure.mongodb;

import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlotsRepository;
import com.eventstore.scheduling.domain.readmodel.availableslots.AvailableSlot;
import com.eventstore.scheduling.domain.doctorday.SlotId;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.vavr.collection.List;
import lombok.*;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import java.time.LocalDate;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoAvailableSlotsRepository implements AvailableSlotsRepository {
  private final MongoCollection<SlotRow> collection;

  public MongoAvailableSlotsRepository(MongoDatabase database) {
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(
                new CustomCodecProvider(), PojoCodecProvider.builder().automatic(true).build()));
    collection =
        database
            .withCodecRegistry(pojoCodecRegistry)
            .getCollection("available_slots", SlotRow.class);
  }

  @Override
  public List<AvailableSlot> getAvailableSlotsOn(LocalDate date) {
    return List.ofAll(collection.find(and(eq("data.date", date.toString()), eq("hidden", false))))
        .map(SlotRow::getData);
  }

  @Override
  public void addSlot(AvailableSlot availableSlot) {
    collection.insertOne(new SlotRow(new ObjectId(), availableSlot, false));
  }

  @Override
  public void hideSlot(SlotId slotId) {
    setStatus(slotId, true);
  }

  @Override
  public void showSlot(SlotId slotId) {
    setStatus(slotId, false);
  }

  @Override
  public void deleteSlot(SlotId slotId) {
    collection.findOneAndDelete(eq("data.slotId", slotId.getValue()));
  }

  private void setStatus(SlotId slotId, boolean hidden) {
    collection.findOneAndUpdate(eq("data.slotId", slotId.getValue()), set("hidden", hidden));
  }

  @EqualsAndHashCode
  @ToString
  @AllArgsConstructor
  @Getter
  @Setter
  @NoArgsConstructor
  public static class SlotRow {
    private ObjectId id;
    private AvailableSlot data;
    private Boolean hidden;
  }
}
