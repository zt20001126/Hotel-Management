package com.homestay.module.external.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 只读小程序订单投影，不包含宾客隐私字段。 */
@Data
public class MiniProgramOrderProjection {
    /** 小程序订单标识。 */ private Long id;
    /** 小程序订单号。 */ private String orderNo;
    /** 实际房间，未入住时在房型仅有一间房的情况下推导。 */ private Long roomId;
    /** 计划入住日期。 */ private LocalDate checkInDate;
    /** 计划退房日期。 */ private LocalDate checkOutDate;
    /** 入住人数。 */ private Integer guestCount;
    /** 订单房费。 */ private BigDecimal totalPrice;
    /** 已支付房费。 */ private BigDecimal paidAmount;
    /** 小程序订单原始状态。 */ private String status;
    /** 创建时间，用于合并订单的排序。 */ private LocalDateTime createTime;
}
