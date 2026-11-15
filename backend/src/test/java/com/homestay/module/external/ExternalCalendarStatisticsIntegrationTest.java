package com.homestay.module.external;

import com.homestay.common.exception.BusinessException;
import com.homestay.module.external.service.ExternalService;
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

/** 日历区间、历史房间与报表筛选的数据库集成测试。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "EXTERNAL_INTEGRATION_TEST", matches = "true")
class ExternalCalendarStatisticsIntegrationTest {
    @Autowired private ExternalService service;
    @Autowired private JdbcTemplate jdbc;
    private long room(int offset) { return jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1 OFFSET " + offset, Long.class); }
    private long platform(int offset) { return jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1 OFFSET " + offset, Long.class); }
    private void insert(String no, long p, Long a, long r, String start, String end, String status, String amount) {
        jdbc.update("INSERT INTO external_order(order_no,platform_id,anchor_id,room_id,check_in_date,check_out_date,guest_count,total_amount,platform_fee,anchor_fee,status) VALUES (?,?,?,?,?,?,1,?,0,0,?)",
                no, p, a, r, start, end, new BigDecimal(amount), status);
    }

    @Test @Transactional void calendarUsesStayNightsAndKeepsDeletedRoomHistory() {
        long r1 = room(0), r2 = room(1), p1 = platform(0), p2 = platform(1);
        String roomNumber = jdbc.queryForObject("SELECT room_number FROM room WHERE id=?", String.class, r1);
        String no = UUID.randomUUID().toString();
        insert(no + "a", p1, null, r1, "2070-12-30", "2071-01-02", "BOOKED", "100");
        insert(no + "b", p2, null, r2, "2071-01-01", "2071-01-02", "CHECKED_IN", "200");
        insert(no + "c", p1, null, r2, "2071-01-02", "2071-01-03", "CHECKED_OUT", "300");
        insert(no + "d", p1, null, r1, "2071-01-02", "2071-01-03", "CANCELLED", "400");
        LocalDate jan1 = LocalDate.of(2071, 1, 1), jan2 = jan1.plusDays(1);
        var firstDay = service.calendar(jan1, jan1, null);
        assertEquals(2, firstDay.size());
        assertTrue(firstDay.stream().anyMatch(order -> order.roomId().equals(r1) && order.status().equals("BOOKED")));
        assertTrue(firstDay.stream().anyMatch(order -> order.roomId().equals(r2) && order.status().equals("CHECKED_IN")));
        var secondDay = service.calendar(jan2, jan2, null);
        assertEquals(1, secondDay.size()); assertEquals("CHECKED_OUT", secondDay.get(0).status());
        assertTrue(service.calendar(jan2, jan2, p2).isEmpty());
        assertEquals(1, service.calendar(jan1, jan1, p1).size());
        jdbc.update("UPDATE room SET deleted=1 WHERE id=?", r1);
        var historical = service.calendar(jan1, jan1, p1);
        assertEquals(1, historical.size()); assertEquals(roomNumber, historical.get(0).roomName());
    }

    @Test @Transactional void reportFiltersCombineAndInvalidRangesReject() {
        long r1 = room(0), r2 = room(1), p1 = platform(0), p2 = platform(1);
        jdbc.update("INSERT INTO external_anchor(name,status) VALUES (?,1)", "qa-" + UUID.randomUUID());
        Long anchor = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        String no = UUID.randomUUID().toString();
        insert(no + "a", p1, anchor, r1, "2072-02-28", "2072-03-02", "CHECKED_IN", "3.00");
        insert(no + "b", p2, null, r2, "2072-02-29", "2072-03-01", "CHECKED_OUT", "5.00");
        insert(no + "c", p1, anchor, r1, "2072-03-02", "2072-03-03", "BOOKED", "9.00");
        LocalDate leapDay = LocalDate.of(2072, 2, 29);
        var all = service.statistics(leapDay, leapDay, null, null, null);
        assertEquals(2, all.total().orderCount()); assertEquals(2, all.total().nights());
        assertEquals(0, new BigDecimal("6.00").compareTo(all.total().totalAmount()));
        var filtered = service.statistics(leapDay, leapDay, p1, anchor, r1);
        assertEquals(1, filtered.total().orderCount()); assertEquals(1, filtered.total().nights());
        assertEquals(0, new BigDecimal("1.00").compareTo(filtered.total().totalAmount()));
        assertEquals(0, service.statistics(leapDay, leapDay, p1, anchor, r2).total().orderCount());
        assertThrows(BusinessException.class, () -> service.statistics(leapDay, leapDay.minusDays(1), null, null, null));
        assertThrows(BusinessException.class, () -> service.calendar(leapDay, leapDay.plusDays(366), null));
    }
}
