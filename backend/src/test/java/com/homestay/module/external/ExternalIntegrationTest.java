package com.homestay.module.external;

import com.homestay.common.util.JwtUtil;
import com.homestay.module.external.dto.ExternalOrderRequest;
import com.homestay.module.external.service.ExternalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 在隔离的 MySQL 测试库中验证权限、事务与房间并发冲突。 */
@SpringBootTest @AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="EXTERNAL_INTEGRATION_TEST", matches="true")
class ExternalIntegrationTest {
    @Autowired private ExternalService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwt;

    @Test @Transactional void orderUpdateViaHttpPersistsValuesAndConflictDoesNotPartiallyWrite() throws Exception {
        String owner = "Bearer " + jwt.generateToken(1L, "owner-test", "employee", "owner");
        Long r1 = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        Long r2 = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1 OFFSET 1", Long.class);
        Long p = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1", Long.class);
        String no = UUID.randomUUID().toString(), occupiedNo = UUID.randomUUID().toString();
        mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(p, r1, no, "2065-08-01", "2065-08-03"))).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content(orderBody(p, r2, occupiedNo, "2065-08-03", "2065-08-05"))).andExpect(jsonPath("$.code").value(200));
        Long id = jdbc.queryForObject("SELECT id FROM external_order WHERE platform_order_no=?", Long.class, no);
        String conflict = orderBody(p, r2, no, "2065-08-04", "2065-08-06").replace("\"totalAmount\":100", "\"totalAmount\":999");
        mvc.perform(put("/external/orders/{id}", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content(conflict)).andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/external/orders/{id}", id).header("Authorization", owner))
                .andExpect(jsonPath("$.data.roomId").value(r1)).andExpect(jsonPath("$.data.totalAmount").value(100));
        String updated = orderBody(p, r2, no, "2065-08-05", "2065-08-06")
                .replace("\"totalAmount\":100", "\"totalAmount\":188.88")
                .replace("\"platformFee\":5", "\"platformFee\":8.88")
                .replaceFirst("\\{", "{\"id\":" + id + ",\"nights\":1,\"netProfit\":180,");
        mvc.perform(put("/external/orders/{id}", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content(updated)).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/external/orders/{id}", id).header("Authorization", owner))
                .andExpect(jsonPath("$.data.roomId").value(r2)).andExpect(jsonPath("$.data.totalAmount").value(188.88))
                .andExpect(jsonPath("$.data.netProfit").value(180));
    }

    @Test @Transactional void identicalAnchorNamesRemainDistinctInReport() {
        Long room = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        Long platform = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1", Long.class);
        String name = "same-" + UUID.randomUUID();
        jdbc.update("INSERT INTO external_anchor(name,status) VALUES (?,1)", name);
        Long first = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("INSERT INTO external_anchor(name,status) VALUES (?,1)", name);
        Long second = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        String prefix = UUID.randomUUID().toString();
        insertStatisticOrder(prefix + "a", platform, first, room, "2051-01-01", "2051-01-02", "10.00", "1.00", "2.00", "CHECKED_IN");
        insertStatisticOrder(prefix + "b", platform, second, room, "2051-01-02", "2051-01-03", "10.00", "1.00", "2.00", "CHECKED_OUT");
        var result = service.statistics(LocalDate.of(2051, 1, 1), LocalDate.of(2051, 1, 31), null, null, null);
        assertEquals(2, result.anchors().size());
        assertTrue(result.anchors().stream().allMatch(item -> item.name().equals(name)));
        assertTrue(result.anchors().stream().allMatch(item -> item.orderCount() == 1));
    }

    @Test @Transactional void statisticsIncludeOnlyStayedOrdersAndKeepDimensionsIndependent() throws Exception {
        Long room1 = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        Long room2 = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1 OFFSET 1", Long.class);
        Long platform1 = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1", Long.class);
        Long platform2 = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1 OFFSET 1", Long.class);
        jdbc.update("INSERT INTO external_anchor(name,status) VALUES (?,1)", "qa-" + UUID.randomUUID());
        Long anchor = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        String prefix = UUID.randomUUID().toString();
        insertStatisticOrder(prefix + "a", platform1, anchor, room1, "2050-01-31", "2050-02-02", "10.01", "1.01", "2.00", "CHECKED_IN");
        insertStatisticOrder(prefix + "b", platform2, null, room2, "2050-02-01", "2050-02-02", "5.00", "6.00", "0.00", "CHECKED_OUT");
        insertStatisticOrder(prefix + "c", platform1, null, room2, "2050-02-02", "2050-02-03", "999.00", "0.00", "0.00", "BOOKED");
        insertStatisticOrder(prefix + "d", platform1, null, room1, "2050-02-03", "2050-02-04", "999.00", "0.00", "0.00", "CANCELLED");
        var report = service.statistics(LocalDate.of(2050, 2, 1), LocalDate.of(2050, 2, 28), null, null, null);
        assertEquals(2, report.total().orderCount()); assertEquals(2, report.total().nights());
        assertEquals(new BigDecimal("10.00"), report.total().totalAmount());
        assertEquals(new BigDecimal("6.50"), report.total().platformFee());
        assertEquals(new BigDecimal("1.00"), report.total().anchorFee());
        assertEquals(new BigDecimal("2.50"), report.total().netProfit());
        assertEquals(2, report.platforms().size()); assertEquals(2, report.anchors().size()); assertEquals(2, report.rooms().size());
        var selected = service.statistics(LocalDate.of(2050, 2, 1), LocalDate.of(2050, 2, 28), platform2, null, null);
        assertEquals(new BigDecimal("-1.00"), selected.total().netProfit());
        assertEquals(1, selected.total().orderCount());
    }

    private void insertStatisticOrder(String no, Long platform, Long anchor, Long room, String start, String end,
                                      String total, String fee, String commission, String status) {
        jdbc.update("INSERT INTO external_order(order_no,platform_id,anchor_id,room_id,check_in_date,check_out_date,guest_count,total_amount,platform_fee,anchor_fee,status) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                no, platform, anchor, room, start, end, 1, new BigDecimal(total), new BigDecimal(fee), new BigDecimal(commission), status);
    }

    @Test @Transactional void catalogCanClearRemarkAndRejectInvalidStatus() throws Exception {
        String owner = "Bearer " + jwt.generateToken(1L, "owner-test", "employee", "owner");
        String name = "anchor-" + UUID.randomUUID();
        mvc.perform(post("/external/anchors").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"remark\":\"old\",\"status\":1}"))
                .andExpect(jsonPath("$.code").value(200));
        Long id = jdbc.queryForObject("SELECT id FROM external_anchor WHERE name=?", Long.class, name);
        mvc.perform(put("/external/anchors/{id}", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"remark\":null,\"status\":1}"))
                .andExpect(jsonPath("$.code").value(200));
        assertNull(jdbc.queryForObject("SELECT remark FROM external_anchor WHERE id=?", String.class, id));
        mvc.perform(put("/external/anchors/{id}", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"status\":9}"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test @Transactional void conflictCancellationIdempotencyAndHistoricalNames() throws Exception {
        String owner = "Bearer " + jwt.generateToken(1L, "owner-test", "employee", "owner");
        Long roomId = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        Long otherRoomId = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1 OFFSET 1", Long.class);
        Long platformId = jdbc.queryForObject("SELECT id FROM external_platform ORDER BY id LIMIT 1", Long.class);
        Integer originalMiniOrders = jdbc.queryForObject("SELECT COUNT(*) FROM booking_order", Integer.class);
        String externalNo = UUID.randomUUID().toString();
        String body = orderBody(platformId, roomId, externalNo, "2047-05-01", "2047-05-03");
        mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        Long id = jdbc.queryForObject("SELECT id FROM external_order WHERE platform_order_no=?", Long.class, externalNo);
        mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(platformId, roomId, UUID.randomUUID().toString(), "2047-05-02", "2047-05-04")))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(platformId, otherRoomId, externalNo, "2047-05-01", "2047-05-03")))
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(platformId, roomId, UUID.randomUUID().toString(), "2047-05-03", "2047-05-04")))
                .andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/external/orders/{id}/status", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CHECKED_OUT\"}")).andExpect(jsonPath("$.code").value(400));
        jdbc.update("UPDATE external_platform SET name='renamed-platform' WHERE id=?", platformId);
        mvc.perform(get("/external/orders/{id}", id).header("Authorization", owner))
                .andExpect(jsonPath("$.data.platformName").value("renamed-platform"));
        mvc.perform(post("/external/orders/{id}/status", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/external/orders/{id}/status", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}")).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(platformId, roomId, UUID.randomUUID().toString(), "2047-05-01", "2047-05-03")))
                .andExpect(jsonPath("$.code").value(200));
        assertEquals(originalMiniOrders, jdbc.queryForObject("SELECT COUNT(*) FROM booking_order", Integer.class));
    }

    private static String orderBody(Long platformId, Long roomId, String platformNo, String start, String end) {
        return "{\"platformId\":" + platformId + ",\"roomId\":" + roomId + ",\"platformOrderNo\":\"" + platformNo +
                "\",\"checkInDate\":\"" + start + "\",\"checkOutDate\":\"" + end +
                "\",\"guestCount\":2,\"totalAmount\":100,\"platformFee\":5,\"anchorFee\":0}";
    }

    @Test @Transactional void orderLifecycleAndMonthlyReport() throws Exception {
        String owner = "Bearer " + jwt.generateToken(1L, "owner-test", "employee", "owner");
        Long roomId = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 LIMIT 1", Long.class);
        Long platformId = jdbc.queryForObject("SELECT id FROM external_platform LIMIT 1", Long.class);
        String externalNo = UUID.randomUUID().toString();
        String body = "{\"platformId\":" + platformId + ",\"roomId\":" + roomId +
                ",\"platformOrderNo\":\"" + externalNo + "\",\"checkInDate\":\"2046-05-30\",\"checkOutDate\":\"2046-06-02\"," +
                "\"guestCount\":2,\"totalAmount\":1000.00,\"platformFee\":100.00,\"anchorFee\":0}";
        mvc.perform(post("/external/orders").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(200));
        Long id = jdbc.queryForObject("SELECT id FROM external_order WHERE platform_order_no=?", Long.class, externalNo);
        mvc.perform(get("/external/orders").header("Authorization", owner).param("keyword", externalNo))
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.records[0].id").value(id));
        mvc.perform(get("/external/calendar").header("Authorization", owner).param("startDate", "2046-05-30").param("endDate", "2046-05-31"))
                .andExpect(jsonPath("$.data[0].id").value(id));
        mvc.perform(post("/external/orders/{id}/status", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CHECKED_IN\"}")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/external/statistics").header("Authorization", owner).param("startDate", "2046-05-01").param("endDate", "2046-05-31"))
                .andExpect(jsonPath("$.data.total.orderCount").value(1)).andExpect(jsonPath("$.data.total.nights").value(2))
                .andExpect(jsonPath("$.data.total.totalAmount").value(666.67)).andExpect(jsonPath("$.data.total.netProfit").value(600));
        mvc.perform(post("/external/orders/{id}/status", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CHECKED_OUT\"}")).andExpect(jsonPath("$.code").value(200));
    }

    @Test void concurrentReservationsAndOwnerAuthorization() throws Exception {
        String owner = "Bearer " + jwt.generateToken(1L, "owner-test", "employee", "owner");
        String guest = "Bearer " + jwt.generateToken(2L, "guest-test", "guest", "guest");
        mvc.perform(get("/external/platforms").header("Authorization", owner)).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/external/platforms").header("Authorization", guest)).andExpect(jsonPath("$.code").value(403));
        Long roomId = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 LIMIT 1", Long.class);
        Long platformId = jdbc.queryForObject("SELECT id FROM external_platform LIMIT 1", Long.class);
        ExternalOrderRequest request = new ExternalOrderRequest();
        request.setRoomId(roomId); request.setPlatformId(platformId); request.setGuestCount(2);
        request.setCheckInDate(LocalDate.of(2045, 1, 1)); request.setCheckOutDate(LocalDate.of(2045, 1, 3));
        request.setTotalAmount(new BigDecimal("100.00")); request.setPlatformFee(new BigDecimal("5.00"));
        request.setAnchorFee(BigDecimal.ZERO);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> create = () -> { start.await(); try { service.createOrder(request); return true; } catch (RuntimeException e) { return false; } };
        try {
            Future<Boolean> first = executor.submit(create), second = executor.submit(create); start.countDown();
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM external_order WHERE room_id=? AND check_in_date='2045-01-01'", Integer.class, roomId));
        } finally { executor.shutdownNow(); jdbc.update("DELETE FROM external_order WHERE room_id=? AND check_in_date='2045-01-01'", roomId); }
    }
}
