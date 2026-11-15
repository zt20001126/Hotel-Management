package com.homestay.module.room.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 房型展示信息及可持久化的图片引用。 */
@Data
public class RoomTypeVO {
    /** 房型标识。 */ private Long id;
    /** 房型名称。 */ private String name;
    /** 房型描述。 */ private String description;
    /** 基础价格。 */ private BigDecimal basePrice;
    /** 周末价格。 */ private BigDecimal weekendPrice;
    /** 节假日价格。 */ private BigDecimal holidayPrice;
    /** 最大入住人数。 */ private Integer maxGuests;
    /** 床位数量。 */ private Integer bedCount;
    /** 床型描述。 */ private String bedType;
    /** 房间面积。 */ private Double area;
    /** 房型设施。 */ private String facilities;
    /** 可供展示的图片地址 JSON 数组。 */ private String images;
    /** 可持久化的图片引用，顺序与展示地址一致。 */ private List<String> imageRefs;
    /** 上架状态。 */ private Integer status;
    /** 排序序号。 */ private Integer sortOrder;
    /** 创建时间。 */ private LocalDateTime createTime;
    /** 更新时间。 */ private LocalDateTime updateTime;
    /** 删除标记。 */ private Integer deleted;
}
