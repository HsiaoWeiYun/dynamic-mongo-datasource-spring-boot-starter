package com.weiyunhsiao.module.dmdsbs.creator;

import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;

@FunctionalInterface
public interface MongoClientSettingsBuilderCustomizerCreator {
    MongoClientSettingsBuilderCustomizer customizer(String name);
}
