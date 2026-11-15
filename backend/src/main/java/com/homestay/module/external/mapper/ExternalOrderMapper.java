package com.homestay.module.external.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homestay.module.external.entity.ExternalOrder;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;

/** 第三方订单数据访问；房间锁用于串行化第三方订单写入。 */
@Mapper public interface ExternalOrderMapper extends BaseMapper<ExternalOrder> {
    @Select("SELECT id FROM room WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Long lockRoom(@Param("id") Long id);
    @Select("SELECT COUNT(*) FROM external_order WHERE room_id = #{roomId} AND status <> 'CANCELLED' AND check_in_date < #{end} AND check_out_date > #{start} AND (#{excluded} IS NULL OR id <> #{excluded}) FOR UPDATE")
    long countConflicts(@Param("roomId") Long roomId, @Param("start") LocalDate start,
                        @Param("end") LocalDate end, @Param("excluded") Long excluded);
}
