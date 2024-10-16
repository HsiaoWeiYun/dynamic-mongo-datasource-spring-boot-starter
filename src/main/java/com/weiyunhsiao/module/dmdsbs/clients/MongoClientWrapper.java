package com.weiyunhsiao.module.dmdsbs.clients;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;

@Getter
public class MongoClientWrapper <C extends MongoClient> {

    private final C client;

    private final String databaseName;

    private final String clientName;

    public MongoClientWrapper(C client, String databaseName, String clientName) {
        this.client = client;
        this.databaseName = databaseName;
        this.clientName = clientName;
    }

    public MongoDatabase getDatabase() {
        return client.getDatabase(databaseName);
    }
}
