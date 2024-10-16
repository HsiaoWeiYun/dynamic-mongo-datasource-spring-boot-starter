package com.weiyunhsiao.module.dmdsbs.autoconfigure;

import com.weiyunhsiao.module.dmdsbs.DynamicRoutingMongoDatabaseFactory;
import com.weiyunhsiao.module.dmdsbs.annotation.MongoDS;
import com.weiyunhsiao.module.dmdsbs.aop.DynamicMongoDataSourceAnnotationAdvisor;
import com.weiyunhsiao.module.dmdsbs.aop.DynamicMongoDataSourceAnnotationInterceptor;
import com.weiyunhsiao.module.dmdsbs.autoconfigure.property.DynamicRoutingMongoDatasourceProperties;
import com.weiyunhsiao.module.dmdsbs.creator.DefaultMongoClientCreator;
import com.weiyunhsiao.module.dmdsbs.creator.MongoClientSettingsBuilderCustomizerCreator;
import com.weiyunhsiao.module.dmdsbs.provider.DefaultDynamicMongoDataSourceProvider;
import com.weiyunhsiao.module.dmdsbs.provider.DynamicMongoDataSourceProvider;
import com.weiyunhsiao.module.dmdsbs.resolver.MongoDataSourceAnnotationValueResolver;
import com.weiyunhsiao.module.dmdsbs.resolver.TenantAnnotationValueResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.Advisor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.data.mongodb.MongoDatabaseFactory;

import java.util.List;
import java.util.stream.Collectors;

@AutoConfiguration
@AutoConfigureBefore(MongoAutoConfiguration.class)
@EnableConfigurationProperties({DynamicRoutingMongoDatasourceProperties.class})
@Slf4j
public class DynamicRoutingMongoDatasourceAutoConfigure {

    @Primary
    @Bean
    public MongoDatabaseFactory dynamicRoutingMongoDatabaseFactory(DynamicRoutingMongoDatasourceProperties properties,
                                                                   List<DynamicMongoDataSourceProvider> providers,
                                                                   MongoDataSourceAnnotationValueResolver mongoDataSourceAnnotationValueResolver) {
        DynamicRoutingMongoDatabaseFactory factory = new DynamicRoutingMongoDatabaseFactory(providers, mongoDataSourceAnnotationValueResolver);
        factory.setStrategy(properties.getStrategy());
        factory.setPrimary(properties.getPrimary());
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean
    public DynamicMongoDataSourceProvider defaultDynamicMongoDataSourceProvider(ObjectProvider<MongoClientSettingsBuilderCustomizerCreator> creators,
                                                                                DynamicRoutingMongoDatasourceProperties properties) {
        return new DefaultDynamicMongoDataSourceProvider(
                new DefaultMongoClientCreator(creators.orderedStream().collect(Collectors.toList())),
                properties);
    }

    @Bean
    public Advisor dynamicMongoDataSourceAnnotationAdvisor(MongoDataSourceAnnotationValueResolver mongoDataSourceAnnotationValueResolver) {
        DynamicMongoDataSourceAnnotationInterceptor interceptor = new DynamicMongoDataSourceAnnotationInterceptor(mongoDataSourceAnnotationValueResolver);
        DynamicMongoDataSourceAnnotationAdvisor advisor = new DynamicMongoDataSourceAnnotationAdvisor(interceptor, MongoDS.class);
        advisor.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return advisor;
    }

    @Bean
    @ConditionalOnMissingBean
    public MongoDataSourceAnnotationValueResolver mongoDataSourceAnnotationValueResolver() {
        return new TenantAnnotationValueResolver();
    }

}
