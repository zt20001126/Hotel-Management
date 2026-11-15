package com.homestay.module.external.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 第三方平台主播持久化对象。 */
@Data @TableName("external_anchor")
public class ExternalAnchor {
    /** 主播标识。 */ @TableId(type=IdType.AUTO) private Long id;
    /** 主播名称。 */ private String name;
    /** 启用状态。 */ private Integer status;
    /** 主播备注。 */ private String remark;
}
