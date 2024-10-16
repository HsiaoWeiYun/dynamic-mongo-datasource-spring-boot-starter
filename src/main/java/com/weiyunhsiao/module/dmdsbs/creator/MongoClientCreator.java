package com.weiyunhsiao.module.dmdsbs.creator;


import com.mongodb.client.MongoClient;
import com.weiyunhsiao.module.dmdsbs.autoconfigure.property.MongoDataSourceProperty;
import com.weiyunhsiao.module.dmdsbs.clients.MongoClientWrapper;

import java.util.Map;

public interface MongoClientCreator {

    Map<String, MongoClientWrapper<MongoClient>> create(Map<String, MongoDataSourceProperty> dataSourcePropertyMap);

    MongoClientWrapper<MongoClient> create(String clientName, MongoDataSourceProperty properties);
}
