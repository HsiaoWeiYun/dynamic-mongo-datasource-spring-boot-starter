package com.weiyunhsiao.module.dmdsbs.provider;


import com.mongodb.client.MongoClient;
import com.weiyunhsiao.module.dmdsbs.autoconfigure.property.DynamicRoutingMongoDatasourceProperties;
import com.weiyunhsiao.module.dmdsbs.clients.MongoClientWrapper;
import com.weiyunhsiao.module.dmdsbs.creator.MongoClientCreator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class DefaultDynamicMongoDataSourceProvider implements DynamicMongoDataSourceProvider{

    private final MongoClientCreator mongoClientCreator;

    @Getter
    private final DynamicRoutingMongoDatasourceProperties properties;

    public DefaultDynamicMongoDataSourceProvider(MongoClientCreator mongoClientCreator,
                                                 DynamicRoutingMongoDatasourceProperties properties) {
        this.mongoClientCreator = mongoClientCreator;
        this.properties = properties;
    }

    @Override
    public Map<String, MongoClientWrapper<MongoClient>> loadDataSources() {
        return mongoClientCreator.create(properties.getDatasource());
    }
}
