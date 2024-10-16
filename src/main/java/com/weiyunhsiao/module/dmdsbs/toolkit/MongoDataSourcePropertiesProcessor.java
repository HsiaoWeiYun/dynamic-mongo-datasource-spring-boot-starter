package com.weiyunhsiao.module.dmdsbs.toolkit;


import com.weiyunhsiao.module.dmdsbs.autoconfigure.property.DynamicRoutingMongoDatasourceProperties;
import com.weiyunhsiao.module.dmdsbs.autoconfigure.property.MongoDataSourceProperty;
import lombok.SneakyThrows;

import java.util.Map;
import java.util.Objects;

public class MongoDataSourcePropertiesProcessor {

    public static void fillDsPropertiesByGlobal(DynamicRoutingMongoDatasourceProperties dynamicRoutingMongoDatasourceProperties) {
        DynamicRoutingMongoDatasourceProperties.GlobalMongoDataSourceProperty globalProperties = dynamicRoutingMongoDatasourceProperties.getGlobal();
        Map<String, MongoDataSourceProperty> datasourceProperties = dynamicRoutingMongoDatasourceProperties.getDatasource();

        for (Map.Entry<String, MongoDataSourceProperty> entry : datasourceProperties.entrySet()) {
            MongoDataSourceProperty dsProperty = entry.getValue();
            fillPoolProperties(globalProperties.getPool(), dsProperty.getPool());
        }
    }

    private static void fillPoolProperties(DynamicRoutingMongoDatasourceProperties.GlobalMongoDataSourceProperty.MongoConnectionPoolProperties globalPool,
                                           MongoDataSourceProperty.MongoConnectionPoolProperties dsPool) {
        fill(globalPool, dsPool, "connectTimeoutMs", Integer.class);
        fill(globalPool, dsPool, "readTimeoutMs", Integer.class);

        fill(globalPool, dsPool, "maxIdleTimeMs", Integer.class);
        fill(globalPool, dsPool, "waitQueueTimeoutMs", Integer.class);
        fill(globalPool, dsPool, "minPoolSize", Integer.class);
        fill(globalPool, dsPool, "maxPoolSize", Integer.class);
    }

    @SneakyThrows
    private static void fill(Object global, Object ds, String parameterName, Class<?> parameterType) {
        Object dsObj = ds.getClass().getDeclaredMethod(transferToGetterName(parameterName)).invoke(ds);
        if (Objects.isNull(dsObj)) {
            ds.getClass().getDeclaredMethod(transferToSetterName(parameterName), parameterType).invoke(ds, global.getClass().getDeclaredMethod(transferToGetterName(parameterName)).invoke(global));
        }
    }

    private static String transferToSetterName(String parameterName) {
        return "set" + parameterName.substring(0, 1).toUpperCase() + parameterName.substring(1);
    }

    private static String transferToGetterName(String parameterName) {
        return "get" + parameterName.substring(0, 1).toUpperCase() + parameterName.substring(1);
    }

}
