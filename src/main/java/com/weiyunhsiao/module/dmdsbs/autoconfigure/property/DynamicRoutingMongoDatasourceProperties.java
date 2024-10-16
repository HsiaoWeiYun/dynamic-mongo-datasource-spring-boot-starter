package com.weiyunhsiao.module.dmdsbs.autoconfigure.property;

import com.weiyunhsiao.module.dmdsbs.strategy.DynamicDataSourceStrategy;
import com.weiyunhsiao.module.dmdsbs.strategy.LoadBalanceDynamicDataSourceStrategy;
import com.weiyunhsiao.module.dmdsbs.toolkit.MongoDataSourcePropertiesProcessor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = DynamicRoutingMongoDatasourceProperties.PREFIX)
@RefreshScope
@Data
@Slf4j
public class DynamicRoutingMongoDatasourceProperties implements InitializingBean, DisposableBean {

    public static final String PREFIX = "spring.datasource.mongodb.dynamic";

    private String primary = "master";
    private boolean strict = true;

    private Map<String, MongoDataSourceProperty> datasource = new LinkedHashMap<>();
    private Class<? extends DynamicDataSourceStrategy> strategy = LoadBalanceDynamicDataSourceStrategy.class;

    @NestedConfigurationProperty
    private GlobalMongoDataSourceProperty global = new GlobalMongoDataSourceProperty();

    @Override
    public void afterPropertiesSet() throws Exception {
        log.debug("starting fill ds properties by global");
        MongoDataSourcePropertiesProcessor.fillDsPropertiesByGlobal(this);
        log.debug("fill ds properties by global finished");
    }

    @Override
    public void destroy() throws Exception {
        datasource = new LinkedHashMap<>();
    }


    @Data
    public static class GlobalMongoDataSourceProperty{

        @NestedConfigurationProperty
        private MongoConnectionPoolProperties pool = new MongoConnectionPoolProperties();

        @Data
        public static class MongoConnectionPoolProperties {

            private Integer connectTimeoutMs = 6000;
            private Integer readTimeoutMs = 60000;
            private Integer maxIdleTimeMs = 1800000;
            private Integer waitQueueTimeoutMs = 6000;
            private Integer minPoolSize = 24;
            private Integer maxPoolSize = 24;

        }
    }

}
