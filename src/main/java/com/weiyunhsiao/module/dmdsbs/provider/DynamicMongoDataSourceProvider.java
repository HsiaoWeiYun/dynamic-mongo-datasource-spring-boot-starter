package com.weiyunhsiao.module.dmdsbs.provider;


import com.mongodb.client.MongoClient;
import com.weiyunhsiao.module.dmdsbs.clients.MongoClientWrapper;

import java.util.Map;

public interface DynamicMongoDataSourceProvider {
    Map<String, MongoClientWrapper<MongoClient>> loadDataSources();
}
