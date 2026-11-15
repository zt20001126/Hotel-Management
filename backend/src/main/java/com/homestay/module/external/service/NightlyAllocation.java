package com.homestay.module.external.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** 第三方订单金额按计划房晚分摊，保证金额精确到分且总额守恒。 */
public final class NightlyAllocation {
    private static final int MONEY_SCALE = 2;

    private NightlyAllocation() {
    }

    /**
     * 将非负金额分摊到各房晚，余分按住宿日期升序分配。
     *
     * @param amount 订单金额
     * @param nights 计划房晚数
     * @return 按日期顺序排列的金额
     */
    public static List<BigDecimal> allocate(BigDecimal amount, int nights) {
        if (amount == null || amount.signum() < 0 || nights <= 0) {
            throw new IllegalArgumentException("金额和住宿晚数无效");
        }
        long cents = amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY)
                .movePointRight(MONEY_SCALE).longValueExact();
        long base = cents / nights;
        long remainder = cents % nights;
        List<BigDecimal> allocations = new ArrayList<>(nights);
        for (int index = 0; index < nights; index++) {
            long nightlyCents = base + (index < remainder ? 1 : 0);
            allocations.add(BigDecimal.valueOf(nightlyCents, MONEY_SCALE));
        }
        return allocations;
    }
}
