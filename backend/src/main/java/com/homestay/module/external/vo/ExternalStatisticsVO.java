package com.homestay.module.external.vo;

import java.math.BigDecimal;
import java.util.List;

/** 日报及平台、主播、房间维度的经营统计。 */
public record ExternalStatisticsVO(Bucket total, List<Bucket> daily, List<Bucket> platforms,
        List<Bucket> anchors, List<Bucket> rooms) {
    /** 单个维度的金额、房晚和去重订单数。 */
    public record Bucket(String name, long orderCount, long nights, BigDecimal totalAmount,
            BigDecimal platformFee, BigDecimal anchorFee, BigDecimal netProfit) {}
}
