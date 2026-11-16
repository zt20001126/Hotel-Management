package com.homestay.module.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.common.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 小程序下单到合并日历和报表的隔离数据库端到端接口测试。 */
@SpringBootTest @AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "EXTERNAL_INTEGRATION_TEST", matches = "true")
class MiniProgramEndToEndIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwt;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    @Test void combinedCalendarAndReportRejectGuestAndUnauthenticatedRequests() throws Exception {
        String guest = "Bearer " + jwt.generateToken(1L, "guest-qa", "guest", "guest");
        String receptionist = "Bearer " + jwt.generateToken(2L, "reception-qa", "employee", "receptionist");
        for (String route : new String[]{"/external/calendar", "/external/statistics"}) {
            mvc.perform(get(route).param("startDate", "2081-05-01").param("endDate", "2081-05-31"))
                    .andExpect(jsonPath("$.code").value(401));
            mvc.perform(get(route).header("Authorization", guest).param("startDate", "2081-05-01").param("endDate", "2081-05-31"))
                    .andExpect(jsonPath("$.code").value(403));
            mvc.perform(get(route).header("Authorization", receptionist).param("startDate", "2081-05-01").param("endDate", "2081-05-31"))
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    @Test @Transactional void originalBookingPaymentAndCheckInFeedCalendarWithoutMutatingSourceData() throws Exception {
        long room = jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1", Long.class);
        long type = jdbc.queryForObject("SELECT room_type_id FROM room WHERE id=?", Long.class, room);
        jdbc.update("UPDATE room SET status='available' WHERE id=?", room);
        String owner = "Bearer " + jwt.generateToken(1L, "owner-qa", "employee", "owner");
        String guest = "Bearer " + jwt.generateToken(1L, "guest-qa", "guest", "guest");
        String body = "{\"roomTypeId\":" + type + ",\"checkInDate\":\"2081-05-01\",\"checkOutDate\":\"2081-05-03\",\"guestCount\":1,\"guestName\":\"QA\",\"guestPhone\":\"13800000000\"}";
        var created = mvc.perform(post("/order").header("Authorization", guest).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andReturn();
        JsonNode order = json.readTree(created.getResponse().getContentAsString()).path("data");
        long id = order.path("id").asLong();
        assertEquals("pending", order.path("status").asText());
        assertEquals(0, order.path("totalPrice").decimalValue().compareTo(order.path("paidAmount").decimalValue()));
        assertTrue(order.path("roomId").isNull());
        Integer externalCount = jdbc.queryForObject("SELECT COUNT(*) FROM external_order", Integer.class);
        Integer paymentCount = jdbc.queryForObject("SELECT COUNT(*) FROM payment_record WHERE order_id=?", Integer.class, id);
        Map<String, Object> beforeRead = jdbc.queryForMap("SELECT status,room_id,total_price,paid_amount,deleted FROM booking_order WHERE id=?", id);
        JsonNode originalStats = json.readTree(mvc.perform(get("/order/statistics").header("Authorization", owner)).andReturn().getResponse().getContentAsString()).path("data");
        mvc.perform(get("/external/calendar").header("Authorization", owner).param("startDate", "2081-05-01").param("endDate", "2081-05-02").param("platformId", "-1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].status").value("BOOKED")).andExpect(jsonPath("$.data[0].roomId").value(room));
        mvc.perform(get("/external/unified-orders").header("Authorization", owner).param("startDate", "2081-05-01").param("endDate", "2081-05-02").param("platformId", "-1"))
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.records[0].id").value(id))
                .andExpect(jsonPath("$.data.records[0].platformName").value("小程序"));
        mvc.perform(get("/external/statistics").header("Authorization", owner).param("startDate", "2081-05-01").param("endDate", "2081-05-31").param("platformId", "-1"))
                .andExpect(jsonPath("$.data.total.orderCount").value(0));
        assertEquals(beforeRead, jdbc.queryForMap("SELECT status,room_id,total_price,paid_amount,deleted FROM booking_order WHERE id=?", id));
        assertEquals(externalCount, jdbc.queryForObject("SELECT COUNT(*) FROM external_order", Integer.class));
        assertEquals(paymentCount, jdbc.queryForObject("SELECT COUNT(*) FROM payment_record WHERE order_id=?", Integer.class, id));
        assertEquals(originalStats, json.readTree(mvc.perform(get("/order/statistics").header("Authorization", owner)).andReturn().getResponse().getContentAsString()).path("data"));
        mvc.perform(put("/order/{id}/confirm", id).header("Authorization", owner)).andExpect(jsonPath("$.code").value(200));
        mvc.perform(put("/order/{id}/checkin", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"roomId\":" + room + "}")).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/external/calendar").header("Authorization", owner).param("startDate", "2081-05-01").param("endDate", "2081-05-02").param("platformId", "-1"))
                .andExpect(jsonPath("$.data[0].status").value("CHECKED_IN"));
        JsonNode entered = json.readTree(mvc.perform(get("/external/statistics").header("Authorization", owner).param("startDate", "2081-05-01").param("endDate", "2081-05-31").param("platformId", "-1"))
                .andReturn().getResponse().getContentAsString()).path("data").path("total");
        assertEquals(1, entered.path("orderCount").asInt()); assertEquals(2, entered.path("nights").asInt());
        assertEquals(0, order.path("totalPrice").decimalValue().compareTo(entered.path("totalAmount").decimalValue()));
        mvc.perform(put("/order/{id}/checkout", id).header("Authorization", owner)).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/external/calendar").header("Authorization", owner).param("startDate", "2081-05-01").param("endDate", "2081-05-02").param("platformId", "-1"))
                .andExpect(jsonPath("$.data[0].status").value("CHECKED_OUT"));
        mvc.perform(get("/external/statistics").header("Authorization", owner).param("startDate", "2081-05-01").param("endDate", "2081-05-31").param("platformId", "-1"))
                .andExpect(jsonPath("$.data.total.orderCount").value(1));
        // 核对当前口径：退款改变已付金额，但合并报表仍按订单总房费计。
        mvc.perform(post("/payment/refund").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + id + ",\"amount\":1.00,\"remark\":\"qa\"}"))
                .andExpect(jsonPath("$.code").value(200));
        JsonNode afterRefund = json.readTree(mvc.perform(get("/external/statistics").header("Authorization", owner)
                        .param("startDate", "2081-05-01").param("endDate", "2081-05-31").param("platformId", "-1"))
                .andReturn().getResponse().getContentAsString()).path("data").path("total");
        assertEquals(0, order.path("totalPrice").decimalValue().compareTo(afterRefund.path("totalAmount").decimalValue()));
    }
}
