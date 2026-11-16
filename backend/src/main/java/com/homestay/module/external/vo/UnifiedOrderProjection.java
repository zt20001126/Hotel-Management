package com.homestay.module.external.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 统一订单分页查询的只读投影。 */
@Data
public class UnifiedOrderProjection {
    /** 订单标识。 */ private Long id;
    /** 系统订单号。 */ private String orderNo;
    /** 第三方订单号。 */ private String platformOrderNo;
    /** 来源标识。 */ private Long platformId;
    /** 来源名称。 */ private String platformName;
    /** 主播标识。 */ private Long anchorId;
    /** 主播名称。 */ private String anchorName;
    /** 房间标识。 */ private Long roomId;
    /** 房间名称。 */ private String roomName;
    /** 入住日期。 */ private LocalDate checkInDate;
    /** 退房日期。 */ private LocalDate checkOutDate;
    /** 入住人数。 */ private Integer guestCount;
    /** 订单金额。 */ private BigDecimal totalAmount;
    /** 平台费用。 */ private BigDecimal platformFee;
    /** 主播费用。 */ private BigDecimal anchorFee;
    /** 订单状态。 */ private String status;
    /** 订单备注。 */ private String remark;
}
