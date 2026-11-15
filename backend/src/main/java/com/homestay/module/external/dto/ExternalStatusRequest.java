package com.homestay.module.external.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 订单状态变更请求。 */
@Data
public class ExternalStatusRequest {
    /** 目标状态。 */
    @NotBlank private String status;
}
