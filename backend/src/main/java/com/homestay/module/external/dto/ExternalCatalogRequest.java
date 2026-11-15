package com.homestay.module.external.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 平台和主播的维护请求。 */
@Data
public class ExternalCatalogRequest {
    /** 显示名称。 */
    @NotBlank @Size(max = 50) private String name;
    /** 启用状态。 */
    private Integer status = 1;
    /** 主播备注。 */
    @Size(max = 255) private String remark;
}
