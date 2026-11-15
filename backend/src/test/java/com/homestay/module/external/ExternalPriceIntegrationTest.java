package com.homestay.module.external;

import com.homestay.common.util.JwtUtil;
import com.homestay.module.external.service.ExternalService;
import com.homestay.module.external.vo.ExternalStatisticsVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 使用隔离数据库交叉验证第三方订单的金额、期间和统计维度。 */
@SpringBootTest @AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "EXTERNAL_INTEGRATION_TEST", matches = "true")
class ExternalPriceIntegrationTest {
    @Autowired private ExternalService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwt;

    private long room(int offset) { return jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1 OFFSET " + offset, Long.class); }
    private long platform(int offset) { return jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1 OFFSET " + offset, Long.class); }
    private long anchor() {
        jdbc.update("INSERT INTO external_anchor(name,status) VALUES (?,1)", "price-" + UUID.randomUUID());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
    private void insert(String no, long platform, Long anchor, long room, String start, String end,
                        String total, String fee, String commission, String status) {
        jdbc.update("INSERT INTO external_order(order_no,platform_id,anchor_id,room_id,check_in_date,check_out_date,guest_count,total_amount,platform_fee,anchor_fee,status) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                no, platform, anchor, room, start, end, 1, new BigDecimal(total), new BigDecimal(fee), new BigDecimal(commission), status);
    }
    private static void amount(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }
    private static void bucket(ExternalStatisticsVO.Bucket b, long count, long nights, String total, String fee, String commission, String profit) {
        assertEquals(count, b.orderCount()); assertEquals(nights, b.nights());
        amount(total, b.totalAmount()); amount(fee, b.platformFee()); amount(commission, b.anchorFee()); amount(profit, b.netProfit());
    }

    @Test @Transactional void crossYearZeroNegativeAndMultiDimensionalTotals() {
        long p1 = platform(0), p2 = platform(1), r1 = room(0), r2 = room(1), a1 = anchor();
        String n = UUID.randomUUID().toString();
        insert(n + "a", p1, a1, r1, "2049-12-30", "2050-01-02", "10.01", "1.01", "2.00", "CHECKED_IN");
        insert(n + "b", p2, null, r2, "2050-01-01", "2050-01-03", "0.01", "0.03", "0.00", "CHECKED_OUT");
        insert(n + "c", p1, a1, r1, "2050-01-02", "2050-01-03", "100.00", "3.50", "5.25", "CHECKED_OUT");
        insert(n + "d", p1, null, r1, "2050-01-03", "2050-01-04", "999.00", "0.00", "0.00", "BOOKED");
        insert(n + "e", p1, null, r1, "2050-01-04", "2050-01-05", "999.00", "0.00", "0.00", "CANCELLED");
        LocalDate first = LocalDate.of(2050,1,1), last = LocalDate.of(2050,1,31);
        ExternalStatisticsVO all = service.statistics(first, last, null, null, null);
        bucket(all.total(), 3, 4, "103.34", "3.86", "5.91", "93.57");
        bucket(all.daily().get(0), 2, 2, "3.34", "0.35", "0.66", "2.33");
        bucket(all.daily().get(1), 2, 2, "100.00", "3.51", "5.25", "91.24");
        bucket(service.statistics(first, last, p2, null, null).total(), 1, 2, "0.01", "0.03", "0", "-0.02");
        bucket(service.statistics(first, last, null, a1, null).total(), 2, 2, "103.33", "3.83", "5.91", "93.59");
        bucket(service.statistics(first, last, null, null, r2).total(), 1, 2, "0.01", "0.03", "0", "-0.02");
        bucket(service.statistics(LocalDate.of(2049,12,30), last, null, null, null).total(), 3, 6, "110.02", "4.54", "7.25", "98.23");
    }

    @Test @Transactional void leapDayAndMaximumAmountRemainExact() {
        long p = platform(0), r = room(0);
        String n = UUID.randomUUID().toString();
        insert(n + "a", p, null, r, "2048-02-28", "2048-03-02", "9999999999.99", "0.01", "0.00", "CHECKED_OUT");
        ExternalStatisticsVO feb = service.statistics(LocalDate.of(2048,2,1), LocalDate.of(2048,2,29), null, null, null);
        bucket(feb.total(), 1, 2, "6666666666.66", "0.01", "0", "6666666666.65");
        bucket(service.statistics(LocalDate.of(2048,3,1), LocalDate.of(2048,3,31), null, null, null).total(),
                1, 1, "3333333333.33", "0", "0", "3333333333.33");
        assertEquals("2048-02-29", feb.daily().get(1).name());
        bucket(service.statistics(LocalDate.of(2048,4,1), LocalDate.of(2048,4,30), null, null, null).total(), 0, 0, "0", "0", "0", "0");
    }

    @Test @Transactional void apiRejectsAmountsOutsidePrecisionAndNegativeFees() throws Exception {
        String owner = "Bearer " + jwt.generateToken(1L, "price-qa", "employee", "owner");
        long p = platform(0), r = room(0);
        String base = "{\"platformId\":" + p + ",\"roomId\":" + r + ",\"checkInDate\":\"2052-01-01\",\"checkOutDate\":\"2052-01-02\",\"guestCount\":1,\"totalAmount\":%s,\"platformFee\":%s,\"anchorFee\":%s}";
        for (String body : new String[]{String.format(base,"1.001","0","0"), String.format(base,"10000000000.00","0","0"), String.format(base,"1","-0.01","0")}) {
            mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        }
        mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(base,"1","0","0.01")))
                .andExpect(jsonPath("$.code").value(400));
    }
}
