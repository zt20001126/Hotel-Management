package com.homestay.module.external.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 只读历史房间名称，包括逻辑删除的记录。 */
@Mapper
public interface ExternalRoomMapper {
    @Select("SELECT room_number FROM room WHERE id = #{id}")
    String findRoomNumber(@Param("id") Long id);
}
