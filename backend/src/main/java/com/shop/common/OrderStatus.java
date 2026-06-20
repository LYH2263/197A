package com.shop.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class OrderStatus {

    private OrderStatus() {}

    public static final int PENDING_PAYMENT   = 0;
    public static final int PAID              = 1;
    public static final int SHIPPED           = 2;
    public static final int COMPLETED         = 3;
    public static final int CANCELLED         = 4;

    public static final String OP_CREATE            = "CREATE";
    public static final String OP_PAY               = "PAY";
    public static final String OP_CANCEL            = "CANCEL";
    public static final String OP_SHIP              = "SHIP";
    public static final String OP_RECEIVE           = "RECEIVE";
    public static final String OP_AFTER_SALE_INTENT = "AFTER_SALE_INTENT";

    private static final Map<Integer, Set<Integer>> TRANSITIONS;
    static {
        Map<Integer, Set<Integer>> m = new HashMap<>();
        m.put(PENDING_PAYMENT, Set.of(PAID, CANCELLED));
        m.put(PAID,          Set.of(SHIPPED));
        m.put(SHIPPED,       Set.of(COMPLETED));
        m.put(COMPLETED,     Collections.emptySet());
        m.put(CANCELLED,     Collections.emptySet());
        TRANSITIONS = Collections.unmodifiableMap(m);
    }

    public static boolean canTransition(int from, int to) {
        Set<Integer> allowed = TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static String text(int s) {
        return switch (s) {
            case PENDING_PAYMENT -> "待付款";
            case PAID            -> "已付款";
            case SHIPPED         -> "已发货";
            case COMPLETED       -> "已完成";
            case CANCELLED       -> "已取消";
            default              -> "未知";
        };
    }
}
