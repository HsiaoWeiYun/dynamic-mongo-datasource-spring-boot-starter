package com.weiyunhsiao.module.dmdsbs.strategy;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class RandomDynamicDataSourceStrategy implements DynamicDataSourceStrategy {

    @Override
    public String determineKey(List<String> dsNames) {
        String determineKey = dsNames.get(ThreadLocalRandom.current().nextInt(dsNames.size()));
        log.debug("RandomDynamicDataSourceStrategy determineKey: {}", determineKey);

        return determineKey;
    }
}
