package com.weiyunhsiao.module.dmdsbs.aop;


import com.weiyunhsiao.module.dmdsbs.resolver.MongoDataSourceAnnotationValueResolver;
import com.weiyunhsiao.module.dmdsbs.resolver.MongoDataSourceClassResolver;
import com.weiyunhsiao.module.dmdsbs.toolkit.DynamicDataSourceContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@Slf4j
public class DynamicMongoDataSourceAnnotationInterceptor implements MethodInterceptor {

    private static final String RESOLVER_PREFIX = "#";

    private final MongoDataSourceClassResolver mongoDataSourceClassResolver = new MongoDataSourceClassResolver(true);

    private final MongoDataSourceAnnotationValueResolver mongoDataSourceAnnotationValueResolver;

    public DynamicMongoDataSourceAnnotationInterceptor(MongoDataSourceAnnotationValueResolver mongoDataSourceAnnotationValueResolver) {
        this.mongoDataSourceAnnotationValueResolver = mongoDataSourceAnnotationValueResolver;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        log.debug("DynamicMongoDataSourceAnnotationInterceptor Processing");
        String dsKey = determineDatasourceKey(invocation);
        log.debug("determineDatasourceKey: {}", dsKey);

        DynamicDataSourceContextHolder.push(dsKey);
        try {
            return invocation.proceed();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }

    private String determineDatasourceKey(MethodInvocation invocation) {
        String dsKey = mongoDataSourceClassResolver.findKey(invocation.getMethod(), invocation.getThis());
        return (dsKey.startsWith(RESOLVER_PREFIX)) ? mongoDataSourceAnnotationValueResolver.determineAnnotationValue(invocation, dsKey) : dsKey;
    }
}
