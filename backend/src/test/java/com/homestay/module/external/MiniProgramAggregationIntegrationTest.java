package com.homestay.module.external;

import com.homestay.module.external.service.ExternalService;
import com.homestay.module.external.vo.ExternalOrderVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** 在隔离库核对小程序与第三方订单只读合并的状态和统计。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named="EXTERNAL_INTEGRATION_TEST", matches="true")
class MiniProgramAggregationIntegrationTest {
    @Autowired private ExternalService service;
    @Autowired private JdbcTemplate jdbc;

    private void mini(String no, long type, String start, String end, String state, String total, String paid) {
        jdbc.update("INSERT INTO booking_order(order_no,room_type_id,check_in_date,check_out_date,nights,guest_count,room_price,total_price,paid_amount,status,deleted) VALUES (?,?,?,?,DATEDIFF(?,?),1,?,?,?,?,0)",
                no, type, start, end, end, start, new BigDecimal(total), new BigDecimal(total), new BigDecimal(paid), state);
    }

    @Test @Transactional void calendarAndStatisticsKeepSourcesIndependent() {
        long type = jdbc.queryForObject("SELECT room_type_id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        long room = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 AND room_type_id=?", Long.class, type);
        long platform = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1", Long.class);
        String prefix = UUID.randomUUID().toString();
        mini(prefix + "a", type, "2076-01-01", "2076-01-02", "awaiting_payment", "100", "0");
        mini(prefix + "b", type, "2076-01-02", "2076-01-03", "pending", "100", "100");
        mini(prefix + "c", type, "2076-01-03", "2076-01-05", "checked_in", "200", "200");
        mini(prefix + "d", type, "2076-01-05", "2076-01-06", "checked_out", "100", "100");
        mini(prefix + "e", type, "2076-01-06", "2076-01-07", "cancelled", "100", "100");
        jdbc.update("INSERT INTO external_order(order_no,platform_id,room_id,check_in_date,check_out_date,guest_count,total_amount,platform_fee,anchor_fee,status) VALUES (?,?,?,'2076-01-03','2076-01-04',1,50,5,0,'CHECKED_IN')", prefix + "x", platform, room);
        LocalDate from = LocalDate.of(2076,1,1), to = LocalDate.of(2076,1,31);
        var miniCalendar = service.calendar(from, to, -1L);
        assertEquals(4, miniCalendar.size());
        assertEquals("AWAITING_PAYMENT", miniCalendar.stream().filter(o -> o.orderNo().equals(prefix + "a")).findFirst().orElseThrow().status());
        assertEquals("BOOKED", miniCalendar.stream().filter(o -> o.orderNo().equals(prefix + "b")).findFirst().orElseThrow().status());
        assertTrue(miniCalendar.stream().allMatch(o -> o.roomId().equals(room)));
        assertEquals(5, service.calendar(from, to, null).size());
        var all = service.statistics(from, to, null, null, null).total();
        assertEquals(3, all.orderCount()); assertEquals(4, all.nights());
        assertEquals(0, new BigDecimal("350").compareTo(all.totalAmount()));
        assertEquals(0, new BigDecimal("5").compareTo(all.platformFee()));
        var miniReport = service.statistics(from, to, -1L, null, null).total();
        assertEquals(2, miniReport.orderCount()); assertEquals(3, miniReport.nights());
        assertEquals(0, new BigDecimal("300").compareTo(miniReport.totalAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(miniReport.platformFee()));
        assertEquals(1, service.statistics(from, to, platform, null, null).total().orderCount());
    }

    @Test @Transactional void ambiguousRoomTypesAreNotAssignedArbitrarily() {
        long room = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        long other = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1 OFFSET 1", Long.class);
        long type = jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, room);
        jdbc.update("UPDATE room SET room_type_id=? WHERE id=?", type, other);
        String no = UUID.randomUUID().toString();
        mini(no, type, "2077-02-01", "2077-02-02", "pending", "100", "100");
        ExternalOrderVO result = service.calendar(LocalDate.of(2077,2,1), LocalDate.of(2077,2,1), -1L).get(0);
        assertNull(result.roomId()); assertEquals("待确认房间", result.roomName());
    }

    @Test @Transactional void missingUniqueRoomRemainsUnassignedButActualDeletedRoomKeepsHistory() {
        long room = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        long type = jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, room);
        String number = jdbc.queryForObject("SELECT room_number FROM room WHERE id=?", String.class, room);
        String no = UUID.randomUUID().toString();
        mini(no + "pending", type, "2083-02-01", "2083-02-02", "pending", "100", "100");
        mini(no + "stayed", type, "2083-02-02", "2083-02-03", "checked_out", "100", "100");
        jdbc.update("UPDATE booking_order SET room_id=? WHERE order_no=?", room, no + "stayed");
        jdbc.update("UPDATE room SET deleted=1 WHERE id=?", room);
        var data = service.calendar(LocalDate.of(2083,2,1), LocalDate.of(2083,2,2), -1L);
        var pending = data.stream().filter(o -> o.orderNo().equals(no + "pending")).findFirst().orElseThrow();
        var stayed = data.stream().filter(o -> o.orderNo().equals(no + "stayed")).findFirst().orElseThrow();
        assertNull(pending.roomId()); assertEquals("待确认房间", pending.roomName());
        assertEquals(room, stayed.roomId()); assertEquals(number, stayed.roomName());
        assertEquals(1, service.statistics(LocalDate.of(2083,2,1), LocalDate.of(2083,2,28), -1L, null, room).total().orderCount());
    }

    @Test @Transactional void sameNumericIdsFromDifferentSourcesCountSeparatelyAndAssignedRoomWins() {
        long room = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        long assignedRoom = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1 OFFSET 1", Long.class);
        long type = jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, room);
        long platform = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1", Long.class);
        String no = UUID.randomUUID().toString();
        mini(no + "m", type, "2078-01-01", "2078-01-02", "checked_in", "100", "100");
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("UPDATE booking_order SET room_id=? WHERE id=?", assignedRoom, id);
        jdbc.update("INSERT INTO external_order(id,order_no,platform_id,room_id,check_in_date,check_out_date,guest_count,total_amount,platform_fee,anchor_fee,status) VALUES (?,?,?,?,'2078-01-01','2078-01-02',1,50,0,0,'CHECKED_IN')", id, no + "x", platform, room);
        LocalDate day = LocalDate.of(2078,1,1);
        var miniCalendar = service.calendar(day, day, -1L);
        assertEquals(assignedRoom, miniCalendar.get(0).roomId());
        var total = service.statistics(day, day, null, null, null).total();
        assertEquals(2, total.orderCount());
        assertEquals(0, new BigDecimal("150").compareTo(total.totalAmount()));
    }

    @Test @Transactional void unifiedOrderListFiltersBothSourcesBeforePagination() {
        long room = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        long type = jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, room);
        long platform = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1", Long.class);
        String prefix = UUID.randomUUID().toString();
        mini(prefix + "m", type, "2088-01-01", "2088-01-02", "pending", "100", "100");
        mini(prefix + "c", type, "2088-01-02", "2088-01-03", "cancelled", "100", "100");
        jdbc.update("INSERT INTO external_order(order_no,platform_id,room_id,check_in_date,check_out_date,guest_count,total_amount,platform_fee,anchor_fee,status) VALUES (?,?,?,'2088-01-01','2088-01-02',1,50,5,0,'BOOKED')", prefix + "e", platform, room);
        LocalDate start = LocalDate.of(2088,1,1), end = LocalDate.of(2088,1,31);
        var first = service.unifiedOrders(1, 2, start, end, null, null, null, null, prefix);
        var second = service.unifiedOrders(2, 2, start, end, null, null, null, null, prefix);
        assertEquals(3, first.getTotal()); assertEquals(2, first.getRecords().size()); assertEquals(1, second.getRecords().size());
        var all = java.util.stream.Stream.concat(first.getRecords().stream(), second.getRecords().stream()).toList();
        assertEquals(2, all.stream().filter(o -> o.platformId().equals(-1L)).count());
        assertEquals(1, all.stream().filter(o -> o.platformId().equals(platform)).count());
        assertEquals("CANCELLED", all.stream().filter(o -> o.orderNo().equals(prefix + "c")).findFirst().orElseThrow().status());
        assertEquals(room, all.stream().filter(o -> o.orderNo().equals(prefix + "m")).findFirst().orElseThrow().roomId());
        assertEquals(2, service.unifiedOrders(1, 10, start, end, -1L, null, null, null, prefix).getTotal());
        assertEquals(1, service.unifiedOrders(1, 10, start, end, platform, null, null, null, prefix).getTotal());
        assertEquals(2, service.unifiedOrders(1, 10, start, end, null, null, null, "BOOKED", prefix).getTotal());
        assertEquals(0, service.unifiedOrders(1, 10, start, end, -1L, 99L, null, null, prefix).getTotal());
    }

    @Test @Transactional void unifiedPaginationOrdersTiesAndAppliesFiltersInBothSources() {
        long room = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        long type = jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, room);
        long platform = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1", Long.class);
        String prefix = UUID.randomUUID().toString();
        mini(prefix + "mini", type, "2089-01-01", "2089-01-03", "pending", "100", "50");
        jdbc.update("UPDATE booking_order SET room_id=?, create_time='2089-01-01 12:00:00' WHERE order_no=?", room, prefix + "mini");
        jdbc.update("INSERT INTO external_order(order_no,platform_order_no,platform_id,room_id,check_in_date,check_out_date,guest_count,total_amount,platform_fee,anchor_fee,status,create_time) VALUES (?,?,?,?, '2089-01-01','2089-01-03',1,50,5,0,'BOOKED','2089-01-01 12:00:00')", prefix + "external", prefix + "platform", platform, room);
        LocalDate from = LocalDate.of(2089, 1, 2), to = LocalDate.of(2089, 1, 2);
        var first = service.unifiedOrders(1, 1, from, to, null, null, room, null, prefix);
        var second = service.unifiedOrders(2, 1, from, to, null, null, room, null, prefix);
        assertEquals(2, first.getTotal());
        assertEquals(prefix + "mini", first.getRecords().get(0).orderNo());
        assertEquals("AWAITING_PAYMENT", first.getRecords().get(0).status());
        assertEquals(prefix + "external", second.getRecords().get(0).orderNo());
        assertEquals(new BigDecimal("45.00"), second.getRecords().get(0).netProfit());
        assertEquals(0, service.unifiedOrders(3, 1, from, to, null, null, room, null, prefix).getRecords().size());
        assertEquals(1, service.unifiedOrders(1, 10, from, to, null, null, room, "AWAITING_PAYMENT", prefix).getTotal());
        assertEquals(1, service.unifiedOrders(1, 10, from, to, platform, null, room, "BOOKED", prefix + "platform").getTotal());
        assertEquals(0, service.unifiedOrders(1, 10, to.plusDays(1), to.plusDays(1), null, null, room, null, prefix).getTotal());
    }

    @Test @Transactional void allOrderStatesAndPartialPaymentsAreMappedCorrectly() {
        long type = jdbc.queryForObject("SELECT room_type_id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        String prefix = UUID.randomUUID().toString();
        String[] states = {"awaiting_payment", "pending", "pending", "confirmed", "checked_in", "checked_out", "cancelled"};
        String[] paid = {"0", "50", "100", "100", "100", "100", "100"};
        for (int i = 0; i < states.length; i++) {
            LocalDate day = LocalDate.of(2082,1,1).plusDays(i);
            mini(prefix + i, type, day.toString(), day.plusDays(1).toString(), states[i], "100", paid[i]);
        }
        mini(prefix + "deleted", type, "2082-01-08", "2082-01-09", "checked_in", "100", "100");
        jdbc.update("UPDATE booking_order SET deleted=1 WHERE order_no=?", prefix + "deleted");
        var calendar = service.calendar(LocalDate.of(2082,1,1), LocalDate.of(2082,1,31), -1L);
        assertEquals(6, calendar.size());
        String[] expected = {"AWAITING_PAYMENT", "AWAITING_PAYMENT", "BOOKED", "BOOKED", "CHECKED_IN", "CHECKED_OUT"};
        for (int i = 0; i < expected.length; i++) {
            String orderNo = prefix + i;
            assertEquals(expected[i], calendar.stream().filter(o -> o.orderNo().equals(orderNo)).findFirst().orElseThrow().status());
        }
        var report = service.statistics(LocalDate.of(2082,1,1), LocalDate.of(2082,1,31), -1L, null, null).total();
        assertEquals(2, report.orderCount()); assertEquals(2, report.nights());
        assertEquals(0, new BigDecimal("200").compareTo(report.totalAmount()));
    }

    @Test @Transactional void mixedSourcesPreserveRemaindersAndCombinedRoomStatistics() {
        long type = jdbc.queryForObject("SELECT room_type_id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        long room = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 AND room_type_id=?", Long.class, type);
        long platform = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1", Long.class);
        jdbc.update("INSERT INTO external_anchor(name,status) VALUES (?,1)", "qa-" + UUID.randomUUID());
        long anchor = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        String prefix = UUID.randomUUID().toString();
        mini(prefix + "m", type, "2084-02-28", "2084-03-02", "checked_in", "100.01", "100.01");
        jdbc.update("INSERT INTO external_order(order_no,platform_id,anchor_id,room_id,check_in_date,check_out_date,guest_count,total_amount,platform_fee,anchor_fee,status) VALUES (?,?,?,?,'2084-02-28','2084-03-02',1,20.01,2.01,1.01,'CHECKED_IN')", prefix + "e", platform, anchor, room);
        LocalDate from = LocalDate.of(2084,2,1), to = LocalDate.of(2084,2,29);
        var all = service.statistics(from, to, null, null, room);
        assertEquals(2, all.total().orderCount()); assertEquals(4, all.total().nights());
        assertEquals(0, new BigDecimal("80.02").compareTo(all.total().totalAmount()));
        assertEquals(0, new BigDecimal("1.34").compareTo(all.total().platformFee()));
        assertEquals(0, new BigDecimal("0.68").compareTo(all.total().anchorFee()));
        assertEquals(0, new BigDecimal("78.00").compareTo(all.total().netProfit()));
        assertEquals(1, all.rooms().size()); assertEquals(2, all.rooms().get(0).orderCount());
        assertEquals(2, all.platforms().size());
        var onlyAnchor = service.statistics(from, to, null, anchor, null).total();
        assertEquals(1, onlyAnchor.orderCount());
        assertEquals(0, new BigDecimal("13.34").compareTo(onlyAnchor.totalAmount()));
        var onlyMini = service.statistics(from, to, -1L, null, null).total();
        assertEquals(1, onlyMini.orderCount());
        assertEquals(0, new BigDecimal("66.68").compareTo(onlyMini.totalAmount()));
    }
}
