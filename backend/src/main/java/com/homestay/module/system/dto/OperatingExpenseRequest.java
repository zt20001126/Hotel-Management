package com.homestay.module.system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/** 经营支出创建和修改请求。 */
@Data
public class OperatingExpenseRequest {
    /** 支出内容。 */
    @NotBlank(message = "支出内容不能为空")
    @Size(max = 500, message = "支出内容不能超过500个字符")
    private String content;
    /** 支出金额。 */
    @NotNull(message = "支出金额不能为空")
    @DecimalMin(value = "0.00", inclusive = false, message = "支出金额必须大于0")
    @Digits(integer = 8, fraction = 2, message = "支出金额最多8位整数和2位小数")
    private BigDecimal amount;
}
