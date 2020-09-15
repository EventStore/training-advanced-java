package com.eventstore.scheduling.infrastructure.mongodb;

import com.eventstore.scheduling.domain.readmodel.archivabledays.ArchivableDaysRepository;
import com.eventstore.scheduling.domain.writemodel.doctorday.DayId;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.vavr.collection.List;
import lombok.*;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import java.time.LocalDate;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoArchivableDaysRepository implements ArchivableDaysRepository {

  private final MongoCollection<ArchivableDay> collection;

  public MongoArchivableDaysRepository(MongoDatabase database) {
    CodecRegistry pojoCodecRegistry =
        fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(
                new CustomCodecProvider(), PojoCodecProvider.builder().automatic(true).build()));
    collection =
        database
            .withCodecRegistry(pojoCodecRegistry)
            .getCollection("archivable_days", ArchivableDay.class);
  }

  @Override
  public void add(LocalDate date, DayId dayId) {
    collection.insertOne(new ArchivableDay(new ObjectId(), date.toString(), dayId.getValue()));
  }

  @Override
  public void remove(DayId dayId) {
    collection.deleteOne(eq("dayId", dayId.getValue()));
  }

  @Override
  public List<DayId> findAll(LocalDate date) {
    return List.ofAll(collection.find(eq("date", date.toString())))
        .map(ArchivableDay::getDayId)
        .map(DayId::new);
  }

  @EqualsAndHashCode
  @ToString
  @AllArgsConstructor
  @Getter
  @Setter
  @NoArgsConstructor
  public static class ArchivableDay {
    private ObjectId id;
    private String date;
    private String dayId;
  }
}
