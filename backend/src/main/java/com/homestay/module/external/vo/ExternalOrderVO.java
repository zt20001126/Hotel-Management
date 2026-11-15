package com.homestay.module.external.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 订单列表及日历显示信息。 */
public record ExternalOrderVO(Long id, String orderNo, String platformOrderNo, Long platformId,
        String platformName, Long anchorId, String anchorName, Long roomId, String roomName,
        LocalDate checkInDate, LocalDate checkOutDate, long nights, Integer guestCount,
        BigDecimal totalAmount, BigDecimal platformFee, BigDecimal anchorFee, BigDecimal netProfit,
        String status, String remark) {}
