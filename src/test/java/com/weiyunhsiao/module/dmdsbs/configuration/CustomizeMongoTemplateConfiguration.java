package com.weiyunhsiao.module.dmdsbs.configuration;


import com.weiyunhsiao.module.dmdsbs.toolkit.MongoToolkit;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.Decimal128;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Configuration
@Slf4j
public class CustomizeMongoTemplateConfiguration {

    @Bean
    public MongoToolkit mongoTemplateToolkit(MongoTemplate mongoTemplate){
        return new MongoToolkit(mongoTemplate);
    }

    @Bean
    public MongoTemplate customizeMongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory, mappingMongoConverter(mongoDatabaseFactory));
    }

    private MongoCustomConversions customConversion() {
        List<Converter<?, ?>> converters = new ArrayList<>();

        converters.add(BigDecimalToDecimal128Converter.INSTANCE);
        converters.add(Decimal128ToBigDecimalConverter.INSTANCE);
        converters.add(OffsetDateTimeToDateConverter.INSTANCE);
        converters.add(DateToOffsetDateTimeConverter.INSTANCE);

        return new MongoCustomConversions(converters);
    }

    private enum BigDecimalToDecimal128Converter implements Converter<BigDecimal, Decimal128> {
        INSTANCE;

        @Override
        public Decimal128 convert(BigDecimal bigDecimal) {
            return new Decimal128(bigDecimal);
        }
    }

    private enum Decimal128ToBigDecimalConverter implements Converter<Decimal128, BigDecimal> {
        INSTANCE;

        @Override
        public BigDecimal convert(Decimal128 decimal128) {
            return decimal128.bigDecimalValue();
        }
    }

    private enum OffsetDateTimeToDateConverter implements Converter<OffsetDateTime, Date> {
        INSTANCE;

        @Override
        public Date convert(OffsetDateTime source) {
            return Date.from(source.toInstant());
        }
    }

    private enum DateToOffsetDateTimeConverter implements Converter<Date, OffsetDateTime> {
        INSTANCE;

        @Override
        public OffsetDateTime convert(Date date) {
            return date.toInstant().atOffset(ZoneOffset.UTC);
        }
    }

    private MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory mongoDatabaseFactory) {
        DefaultDbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDatabaseFactory);
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setFieldNamingStrategy(new SnakeCaseFieldNamingStrategy());
        mappingContext.setAutoIndexCreation(false);

        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        converter.setCustomConversions(customConversion());
        converter.afterPropertiesSet();
        return converter;
    }

}
