package com.weiyunhsiao.module.dmdsbs.toolkit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

public class DynamicDataSourceContextHolder {

    private static final ThreadLocal<Deque<String>> LOOKUP_KEY_HOLDER = ThreadLocal.withInitial(ArrayDeque::new);

    private DynamicDataSourceContextHolder() {
    }

    public static String peek() {
        return Optional.ofNullable(LOOKUP_KEY_HOLDER.get().peek()).orElse("");
    }

    public static String push(String ds) {
        String dataSourceStr = (Objects.isNull(ds) || ds.trim().isEmpty()) ? "" : ds;
        LOOKUP_KEY_HOLDER.get().push(dataSourceStr);
        return dataSourceStr;
    }

    public static void poll() {
        Deque<String> deque = LOOKUP_KEY_HOLDER.get();
        deque.poll();
        if (deque.isEmpty()) {
            LOOKUP_KEY_HOLDER.remove();
        }
    }

    public static void clear() {
        LOOKUP_KEY_HOLDER.remove();
    }


}
