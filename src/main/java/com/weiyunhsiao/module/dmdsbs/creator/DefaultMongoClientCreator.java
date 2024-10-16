package com.weiyunhsiao.module.dmdsbs.creator;


import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.weiyunhsiao.module.dmdsbs.autoconfigure.property.MongoDataSourceProperty;
import com.weiyunhsiao.module.dmdsbs.clients.MongoClientWrapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DefaultMongoClientCreator implements MongoClientCreator {

    private static final Logger log = LoggerFactory.getLogger(DefaultMongoClientCreator.class);

    @Setter
    private final MongoClientSettingsConfigurer configurer;

    public DefaultMongoClientCreator(List<MongoClientSettingsBuilderCustomizerCreator> creator) {
        configurer = new MongoClientSettingsConfigurer(creator);
    }

    /**
     * Creates a map of MongoClientWrappers using the given map of data source properties.
     *
     * @param dataSourcePropertyMap the map of data source properties
     * @return a map of MongoClientWrappers
     */
    @Override
    public Map<String, MongoClientWrapper<MongoClient>> create(@NonNull Map<String, MongoDataSourceProperty> dataSourcePropertyMap) {
        return dataSourcePropertyMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> create(e.getKey(), e.getValue())));
    }

    /**
     * Creates a MongoClientWrapper object with the given clientName and properties.
     *
     * @param clientName  the name of the client
     * @param properties  the MongoDataSourceProperty object containing the client properties
     * @return a MongoClientWrapper object
     */
    @Override
    public MongoClientWrapper<MongoClient> create(@NonNull String clientName, @NonNull MongoDataSourceProperty properties) {
        MongoClientSettings.Builder builder = MongoClientSettings.builder();
        ConnectionString connectionString = new ConnectionString(properties.getUri());
        MongoDataSourceProperty.MongoConnectionPoolProperties poolProperties = properties.getPool();

        configurer.applySocket(poolProperties, builder);
        configurer.applyConnectionString(connectionString, builder);
        configurer.applyConnectionPool(poolProperties, builder);

        MongoDataSourceProperty.ReplicaSetProperties replicaSetProperties = properties.getRs();
        if (Objects.nonNull(replicaSetProperties)) {
            configurer.applyCluster(replicaSetProperties, builder);
        }

        builder.applicationName(clientName);

        configurer.customize(clientName, builder);

        MongoClientSettings mongoClientSettings = builder.build();

        return new MongoClientWrapper<>(MongoClients.create(mongoClientSettings), properties.getDatabase(), clientName);
    }

    /**
     * Helper class to configure MongoClient settings based on the properties from MongoDataSourceProperty.
     */
    static class MongoClientSettingsConfigurer {

        @Getter
        private final List<MongoClientSettingsBuilderCustomizerCreator> creators;

        public MongoClientSettingsConfigurer(List<MongoClientSettingsBuilderCustomizerCreator> creators) {
            this.creators = creators;
        }

        void customize(String clientName, MongoClientSettings.Builder clientSettingsBuilder){
            log.info("{} MongoClientSettingsBuilderCustomizerCreator found", creators.size());
            for(MongoClientSettingsBuilderCustomizerCreator creator : creators){
                creator.customizer(clientName).customize(clientSettingsBuilder);
            }
        }

        void applyConnectionString(@NonNull ConnectionString connectionString, MongoClientSettings.Builder clientSettingsBuilder) {
            clientSettingsBuilder.applyConnectionString(connectionString);
        }

        void applySocket(@NonNull MongoDataSourceProperty.MongoConnectionPoolProperties poolProperties, MongoClientSettings.Builder clientSettingsBuilder) {
            clientSettingsBuilder.applyToSocketSettings(ssb -> {
                ssb.connectTimeout(poolProperties.getConnectTimeoutMs(), TimeUnit.MILLISECONDS);
                ssb.readTimeout(poolProperties.getReadTimeoutMs(), TimeUnit.MILLISECONDS);
            });
        }

        void applyConnectionPool(@NonNull MongoDataSourceProperty.MongoConnectionPoolProperties poolProperties,
                                 MongoClientSettings.Builder clientSettingsBuilder) {
            clientSettingsBuilder.applyToConnectionPoolSettings(cpsb -> {
                cpsb.minSize(poolProperties.getMinPoolSize())
                        .maxSize(poolProperties.getMaxPoolSize())
                        .maxWaitTime(poolProperties.getWaitQueueTimeoutMs(), TimeUnit.MILLISECONDS)
                        .maxConnectionIdleTime(poolProperties.getMaxIdleTimeMs(), TimeUnit.MILLISECONDS);
            });
        }

        void applyCluster(MongoDataSourceProperty.ReplicaSetProperties replicaSetProperties,
                          MongoClientSettings.Builder clientSettingsBuilder) {
            clientSettingsBuilder.applyToClusterSettings(csb -> {
                csb.localThreshold(replicaSetProperties.getLocalThreshold(), TimeUnit.MILLISECONDS);
            });

            if (StringUtils.hasText(replicaSetProperties.getReplicaSetName())) {
                clientSettingsBuilder.applyToClusterSettings(csb -> {
                    csb.requiredReplicaSetName(replicaSetProperties.getReplicaSetName());
                });
            }


            MongoDataSourceProperty.WriteConcernProperties writeConcernProperties = replicaSetProperties.getWriteConcern();
            WriteConcern writeConcern = new WriteConcern(writeConcernProperties.getW())
                    .withJournal(writeConcernProperties.getJournal())
                    .withWTimeout(writeConcernProperties.getWtimeout(), TimeUnit.MILLISECONDS);

            clientSettingsBuilder.writeConcern(writeConcern)
                    .readPreference(ReadPreference.valueOf(replicaSetProperties.getReadPreference().name()))
                    .readConcern(new ReadConcern(replicaSetProperties.getReadConcern()));
        }
    }

}
