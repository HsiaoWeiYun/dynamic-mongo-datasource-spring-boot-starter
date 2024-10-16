package com.weiyunhsiao.module.dmdsbs.resolver;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * MongoDS 內容 解析器, 可階層串連起來
 * */
public abstract class MongoDataSourceAnnotationValueResolver {

    private MongoDataSourceAnnotationValueResolver nextResolver;

    public MongoDataSourceAnnotationValueResolver setNextResolver(MongoDataSourceAnnotationValueResolver nextResolver) {
        this.nextResolver = nextResolver;
        return nextResolver;
    }


    public String determineAnnotationValue(@Nullable MethodInvocation invocation, String key) {
        if (matches(key)) {
            String dsName = doDetermineAnnotationValue(invocation, key);
            if (Objects.isNull(dsName) && Objects.nonNull(nextResolver)) {
                return nextResolver.determineAnnotationValue(invocation, key);
            }
            return dsName;
        }
        if (Objects.nonNull(nextResolver)) {
            return nextResolver.determineAnnotationValue(invocation, key);
        }

        return key;
    }

    public abstract boolean matches(String key);


    protected abstract String doDetermineAnnotationValue(@Nullable MethodInvocation invocation, String key);

}
