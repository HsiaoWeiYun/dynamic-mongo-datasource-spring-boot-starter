package com.weiyunhsiao.module.dmdsbs.strategy;

import java.util.List;

public interface DynamicDataSourceStrategy {
    String determineKey(List<String> dsNames);
}
