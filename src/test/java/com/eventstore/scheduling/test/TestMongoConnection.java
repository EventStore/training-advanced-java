package com.eventstore.scheduling.test;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.BeforeEach;

public interface TestMongoConnection {
  MongoClient mongoClient = MongoClients.create("mongodb://localhost");
  MongoDatabase database = mongoClient.getDatabase("test_projections");

  default MongoDatabase getMongo() {
    return database;
  }

  @BeforeEach
  default void beforeEach() {
    database.drop();
  }
}
