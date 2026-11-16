package com.homestay.module.external.mapper;

import com.homestay.module.external.vo.MiniProgramOrderProjection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

/** 只读小程序订单，房型不唯一时不推测具体房间。 */
@Mapper
public interface MiniProgramOrderReadMapper {
    @Select("SELECT o.id, o.order_no, COALESCE(o.room_id, unique_room.room_id) AS room_id, " +
            "o.check_in_date, o.check_out_date, o.guest_count, o.total_price, o.paid_amount, o.status " +
            "FROM booking_order o LEFT JOIN (SELECT room_type_id, MIN(id) AS room_id FROM room " +
            "WHERE deleted=0 GROUP BY room_type_id HAVING COUNT(*)=1) unique_room ON unique_room.room_type_id=o.room_type_id " +
            "WHERE o.deleted=0 AND o.status IN ('awaiting_payment','pending','confirmed','checked_in','checked_out') " +
            "AND o.check_in_date < #{endExclusive} AND o.check_out_date > #{startDate}")
    List<MiniProgramOrderProjection> selectOverlapping(@Param("startDate") LocalDate startDate,
                                                      @Param("endExclusive") LocalDate endExclusive);

}
