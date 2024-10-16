package com.weiyunhsiao.module.dmdsbs.resolver;


import org.aopalliance.intercept.MethodInvocation;

import java.util.Objects;

public class TenantAnnotationValueResolver extends MongoDataSourceAnnotationValueResolver{

    public static final String PREFIX = "#tenant";

    @Override
    public boolean matches(String key) {
        return key.toLowerCase().startsWith(PREFIX);
    }

    @Override
    protected String doDetermineAnnotationValue(MethodInvocation invocation, String key) {

        //FIXME 修改為基於其餘context or spel 的方案
        //String tenant = PlatformCodeUtil.getCurrentPltCode();
        String tenant = null;
        if(Objects.nonNull(tenant) && !tenant.isEmpty()){
            return tenant + key.substring(PREFIX.length());
        }

        return null;
    }
}
