package com.homestay.module.external.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 第三方平台订单录入请求。 */
@Data
public class ExternalOrderRequest {
    /** 平台订单号。 */
    @Size(max = 100) private String platformOrderNo;
    /** 平台标识。 */
    @NotNull private Long platformId;
    /** 可选主播标识。 */
    private Long anchorId;
    /** 房间标识。 */
    @NotNull private Long roomId;
    /** 计划入住日。 */
    @NotNull private LocalDate checkInDate;
    /** 计划退房日。 */
    @NotNull private LocalDate checkOutDate;
    /** 入住人数。 */
    @NotNull @Min(1) private Integer guestCount;
    /** 成交总额。 */
    @NotNull @DecimalMin("0") @Digits(integer=10, fraction=2) private BigDecimal totalAmount;
    /** 平台手续费。 */
    @NotNull @DecimalMin("0") @Digits(integer=10, fraction=2) private BigDecimal platformFee;
    /** 主播手续费。 */
    @NotNull @DecimalMin("0") @Digits(integer=10, fraction=2) private BigDecimal anchorFee;
    /** 订单备注。 */
    @Size(max=255) private String remark;
}
