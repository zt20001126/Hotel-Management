package com.homestay.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 经营支出持久化实体。 */
@Data
@TableName("operating_expenses")
public class OperatingExpense {
    /** 支出编号。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 支出内容。 */
    private String content;
    /** 支出金额。 */
    private BigDecimal amount;
    /** 创建时间，由数据库维护。 */
    private LocalDateTime createdAt;
    /** 更新时间，由数据库维护。 */
    private LocalDateTime updatedAt;
}
