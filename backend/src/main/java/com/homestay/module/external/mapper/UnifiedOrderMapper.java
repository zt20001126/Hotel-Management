package com.homestay.module.external.mapper;

import com.homestay.module.external.vo.UnifiedOrderProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

/** 两类来源订单的合并查询。 */
@Mapper
public interface UnifiedOrderMapper {
    long count(@Param("start") LocalDate start, @Param("end") LocalDate end,
               @Param("platform") Long platform, @Param("anchor") Long anchor,
               @Param("room") Long room, @Param("status") String status,
               @Param("keyword") String keyword, @Param("miniSource") long miniSource);

    List<UnifiedOrderProjection> selectPage(@Param("start") LocalDate start, @Param("end") LocalDate end,
               @Param("platform") Long platform, @Param("anchor") Long anchor,
               @Param("room") Long room, @Param("status") String status,
               @Param("keyword") String keyword, @Param("miniSource") long miniSource,
               @Param("offset") long offset, @Param("size") int size);
}
