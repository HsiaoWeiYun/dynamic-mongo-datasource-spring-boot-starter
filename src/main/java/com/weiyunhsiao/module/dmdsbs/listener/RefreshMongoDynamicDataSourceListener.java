package com.weiyunhsiao.module.dmdsbs.listener;


import com.mongodb.client.MongoClient;
import com.weiyunhsiao.module.dmdsbs.DynamicRoutingMongoDatabaseFactory;
import com.weiyunhsiao.module.dmdsbs.autoconfigure.property.DynamicRoutingMongoDatasourceProperties;
import com.weiyunhsiao.module.dmdsbs.clients.MongoClientWrapper;
import com.weiyunhsiao.module.dmdsbs.provider.DynamicMongoDataSourceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.data.mongodb.MongoDatabaseFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class RefreshMongoDynamicDataSourceListener implements ApplicationListener<RefreshScopeRefreshedEvent> {

    private final DynamicMongoDataSourceProvider dataSourceProvider;

    private final List<MongoDatabaseFactory> factories;

    private final DynamicRoutingMongoDatasourceProperties properties;

    public RefreshMongoDynamicDataSourceListener(DynamicMongoDataSourceProvider dataSourceProvider, List<MongoDatabaseFactory> factories,
                                                 DynamicRoutingMongoDatasourceProperties properties) {
        this.dataSourceProvider = dataSourceProvider;
        this.factories = factories;
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(RefreshScopeRefreshedEvent event) {
        for (MongoDatabaseFactory factory : factories) {
            if (factory instanceof DynamicRoutingMongoDatabaseFactory) {
                log.info("Starting Refresh Dynamic Routing Mongo Database Factory ");
                DynamicRoutingMongoDatabaseFactory dynamicFactory = (DynamicRoutingMongoDatabaseFactory) factory;
                dynamicFactory.setStrategy(properties.getStrategy());
                dynamicFactory.setPrimary(properties.getPrimary());

                Map<String, MongoClientWrapper<MongoClient>> newClientWrappers = dataSourceProvider.loadDataSources();

                Set<String> dataSourceThatNeedsToBeRemoved = getDataSourceThatNeedsToBeRemoved(dynamicFactory.getWrappedClients().keySet(), newClientWrappers.keySet());

                dataSourceThatNeedsToBeRemoved.forEach(dynamicFactory::unbinding);

                newClientWrappers.forEach(dynamicFactory::binding);
                log.info("Refresh Dynamic Routing Mongo Database Factory Successfully");
            }
        }
    }

    private Set<String> getDataSourceThatNeedsToBeRemoved(Set<String> oldDS, Set<String> newDS) {
        return oldDS.stream().filter(old -> !newDS.contains(old)).collect(Collectors.toSet());
    }

}
