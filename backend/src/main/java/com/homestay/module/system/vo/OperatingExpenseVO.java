package com.homestay.module.system.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 经营支出记录视图。 */
@Data
public class OperatingExpenseVO {
    /** 支出编号。 */ private Long id;
    /** 支出内容。 */ private String content;
    /** 支出金额。 */ private BigDecimal amount;
    /** 创建时间。 */ private LocalDateTime createdAt;
    /** 更新时间。 */ private LocalDateTime updatedAt;
}
