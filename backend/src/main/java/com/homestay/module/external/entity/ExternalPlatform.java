package com.homestay.module.external.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 第三方平台持久化对象。 */
@Data @TableName("external_platform")
public class ExternalPlatform {
    /** 平台标识。 */ @TableId(type=IdType.AUTO) private Long id;
    /** 平台名称。 */ private String name;
    /** 启用状态。 */ private Integer status;
}
