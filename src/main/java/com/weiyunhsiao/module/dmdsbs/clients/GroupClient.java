package com.weiyunhsiao.module.dmdsbs.clients;


import com.mongodb.client.MongoClient;
import com.weiyunhsiao.module.dmdsbs.strategy.DynamicDataSourceStrategy;
import lombok.Data;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GroupClient {
    private String groupName;

    private DynamicDataSourceStrategy dynamicDataSourceStrategy;

    private Map<String, MongoClientWrapper<MongoClient>> clientWrapperMap = new ConcurrentHashMap<>();

    public GroupClient(String groupName, DynamicDataSourceStrategy dynamicDataSourceStrategy) {
        this.groupName = groupName;
        this.dynamicDataSourceStrategy = dynamicDataSourceStrategy;
    }

    public void addClient(String name, MongoClientWrapper<MongoClient> clientWrapper) {
        clientWrapperMap.put(name, clientWrapper);
    }

    public void removeClient(String name) {
        clientWrapperMap.remove(name);
    }

    public MongoClientWrapper<MongoClient> determineClient() {
        return clientWrapperMap.get(determineMongoDsKey());
    }

    public boolean isEmpty(){
        return clientWrapperMap.isEmpty();
    }

    public int size() {
        return clientWrapperMap.size();
    }

    private String determineMongoDsKey() {
        return dynamicDataSourceStrategy.determineKey(new ArrayList<>(clientWrapperMap.keySet()));
    }
}
