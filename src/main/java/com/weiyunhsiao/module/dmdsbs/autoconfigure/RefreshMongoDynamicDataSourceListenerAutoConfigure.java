package com.weiyunhsiao.module.dmdsbs.autoconfigure;


import com.weiyunhsiao.module.dmdsbs.autoconfigure.property.DynamicRoutingMongoDatasourceProperties;
import com.weiyunhsiao.module.dmdsbs.listener.RefreshMongoDynamicDataSourceListener;
import com.weiyunhsiao.module.dmdsbs.provider.DynamicMongoDataSourceProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;

import java.util.List;

@AutoConfiguration
@AutoConfigureAfter(DynamicRoutingMongoDatasourceAutoConfigure.class)
@ConditionalOnBean({DynamicMongoDataSourceProvider.class, DynamicRoutingMongoDatasourceProperties.class})
@ConditionalOnClass(ApplicationListener.class)
public class RefreshMongoDynamicDataSourceListenerAutoConfigure {

    @Bean
    @ConditionalOnMissingBean
    public RefreshMongoDynamicDataSourceListener refreshMongoDynamicDataSourceListener(DynamicMongoDataSourceProvider dynamicMongoDataSourceProvider,
                                                                                       List<MongoDatabaseFactory> mongoDatabaseFactories,
                                                                                       DynamicRoutingMongoDatasourceProperties properties){
        return new RefreshMongoDynamicDataSourceListener(dynamicMongoDataSourceProvider, mongoDatabaseFactories, properties);
    }

}
