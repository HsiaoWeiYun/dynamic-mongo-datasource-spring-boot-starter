package com.weiyunhsiao.module.dmdsbs.autoconfigure.property;

import com.mongodb.ReadConcernLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@EqualsAndHashCode(callSuper = false)
@Data
public class MongoDataSourceProperty extends MongoProperties {

    @NestedConfigurationProperty
    private ReplicaSetProperties rs;

    @NestedConfigurationProperty
    private MongoConnectionPoolProperties pool = new MongoConnectionPoolProperties();

    @Data
    public static class ReplicaSetProperties {
        private String replicaSetName = "";
        private Integer localThreshold = 500;

        private WriteConcernProperties writeConcern = new WriteConcernProperties();

        private ReadPreference readPreference;

        private ReadConcernLevel readConcern = ReadConcernLevel.LOCAL;
    }

    @Data
    public static class MongoConnectionPoolProperties {

        private Integer connectTimeoutMs;
        private Integer readTimeoutMs;
        private Integer maxIdleTimeMs;
        private Integer waitQueueTimeoutMs;
        private Integer minPoolSize;
        private Integer maxPoolSize;

    }

    @Data
    public static class WriteConcernProperties {
        private String w = "majority";
        private Boolean journal = true;
        private Integer wtimeout = 10000;
    }
}
