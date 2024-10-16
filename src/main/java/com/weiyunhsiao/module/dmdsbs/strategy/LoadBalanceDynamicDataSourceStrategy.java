package com.weiyunhsiao.module.dmdsbs.strategy;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LoadBalanceDynamicDataSourceStrategy implements DynamicDataSourceStrategy{

    private final AtomicInteger index = new AtomicInteger(1);

    @Override
    public String determineKey(List<String> dsNames) {
        String determineKey = dsNames.get(Math.abs(index.getAndAdd(1) % dsNames.size()));
        log.debug("LoadBalanceDynamicDataSourceStrategy determineKey: {}", determineKey);

        return determineKey;
    }
}
