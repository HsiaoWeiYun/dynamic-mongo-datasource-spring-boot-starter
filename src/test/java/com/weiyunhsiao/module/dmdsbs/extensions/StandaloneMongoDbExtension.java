package com.weiyunhsiao.module.dmdsbs.extensions;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class StandaloneMongoDbExtension implements BeforeAllCallback, AfterAllCallback {

    public static final String DS1 = "ds1";
    public static final String DS2 = "ds2";
    public static final String DS2_1 = "ds2_1";
    public static final String DS2_2 = "ds2_2";

    private static final String MONGO_VERSION = "mongo:5.0";

    private final GenericContainer<?> mongo1 = new MongoDBContainer(DockerImageName.parse(MONGO_VERSION));

    private static final GenericContainer<?> mongo2_1 = new MongoDBContainer(DockerImageName.parse(MONGO_VERSION));

    private static final GenericContainer<?> mongo2_2 = new MongoDBContainer(DockerImageName.parse(MONGO_VERSION));


    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        mongo1.start();
        mongo2_1.start();
        mongo2_2.start();

        setProperties(mongo1, "ds1", true);
        setProperties(mongo2_1, "ds2_1", false);
        setProperties(mongo2_2, "ds2_2", false);
    }


    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        mongo1.stop();
        mongo2_1.stop();
        mongo2_2.stop();
    }

    private void setProperties(GenericContainer<?> mongoContainer, String dsName, boolean primary) {
        String connectionStringTemplate = "mongodb://localhost:%d/test";


        System.setProperty(String.format("spring.datasource.mongodb.dynamic.datasource.%s.database", dsName), "test");
        System.setProperty(String.format("spring.datasource.mongodb.dynamic.datasource.%s.uri", dsName), String.format(connectionStringTemplate, mongoContainer.getFirstMappedPort()));
        System.setProperty(String.format("spring.datasource.mongodb.dynamic.datasource.%s.field-naming-strategy", dsName), "org.springframework.data.mapping.model.SnakeCaseFieldNamingStrategy");
        System.setProperty(String.format("spring.datasource.mongodb.dynamic.datasource.%s.auto-index-creation", dsName), "false");

        if (primary) {
            System.setProperty("spring.datasource.mongodb.dynamic.primary", dsName);
        }
    }

}
