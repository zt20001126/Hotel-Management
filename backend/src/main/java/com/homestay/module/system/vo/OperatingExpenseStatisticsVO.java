package com.homestay.module.system.vo;

import lombok.Data;
import java.math.BigDecimal;

/** 经营支出汇总视图。 */
@Data
public class OperatingExpenseStatisticsVO {
    /** 汇总金额。 */ private BigDecimal totalAmount;
    /** 汇总笔数。 */ private Long totalCount;
}
