package com.homestay.module.external.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 第三方平台订单持久化对象。 */
@Data @TableName("external_order")
public class ExternalOrder {
    /** 订单标识。 */ @TableId(type=IdType.AUTO) private Long id;
    /** 系统订单号。 */ private String orderNo;
    /** 平台订单号。 */ private String platformOrderNo;
    /** 平台标识。 */ private Long platformId;
    /** 主播标识。 */ private Long anchorId;
    /** 房间标识。 */ private Long roomId;
    /** 计划入住日期。 */ private LocalDate checkInDate;
    /** 计划退房日期。 */ private LocalDate checkOutDate;
    /** 入住人数。 */ private Integer guestCount;
    /** 成交金额。 */ private BigDecimal totalAmount;
    /** 平台手续费。 */ private BigDecimal platformFee;
    /** 主播手续费。 */ private BigDecimal anchorFee;
    /** 订单状态。 */ private String status;
    /** 备注。 */ private String remark;
    /** 创建时间。 */ private LocalDateTime createTime;
    /** 修改时间。 */ private LocalDateTime updateTime;
}
