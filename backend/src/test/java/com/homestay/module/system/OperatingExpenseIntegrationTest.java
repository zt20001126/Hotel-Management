package com.homestay.module.system;

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
import java.util.UUID;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 在已迁移的测试数据库中验证支出接口，事务自动回滚。 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@EnabledIfEnvironmentVariable(named = "EXPENSE_INTEGRATION_TEST", matches = "true")
class OperatingExpenseIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JwtUtil jwt;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void managesExpensesWithFilteringAndAuthorization() throws Exception {
        String owner = "Bearer " + jwt.generateToken(1L, "test", "employee", "owner");
        String other = "Bearer " + jwt.generateToken(2L, "test", "employee", "receptionist");
        String key = "expense-test-" + UUID.randomUUID();
        mvc.perform(post("/operating-expenses").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + key + "\",\"amount\":12.34}"))
                .andExpect(jsonPath("$.code").value(200));
        Long id = jdbc.queryForObject("SELECT id FROM operating_expenses WHERE content = ?", Long.class, key);
        jdbc.update("UPDATE operating_expenses SET created_at = '2026-11-01 00:00:00' WHERE id = ?", id);
        jdbc.update("INSERT INTO operating_expenses(content, amount, created_at) VALUES (?, 5.66, '2026-12-01 00:00:00')", key);
        mvc.perform(get("/operating-expenses").header("Authorization", owner).param("keyword", key).param("page", "1").param("size", "1")
                .param("startTime", "2026-11-01 00:00:00").param("endTime", "2026-12-01 00:00:00"))
                .andExpect(jsonPath("$.data.total").value(1)).andExpect(jsonPath("$.data.records[0].id").value(id));
        mvc.perform(get("/operating-expenses/statistics").header("Authorization", owner).param("keyword", key)
                .param("startTime", "2026-11-01 00:00:00").param("endTime", "2026-12-01 00:00:00"))
                .andExpect(jsonPath("$.data.totalAmount").value(12.34)).andExpect(jsonPath("$.data.totalCount").value(1));
        mvc.perform(put("/operating-expenses/{id}", id).header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + key + "\",\"amount\":20.55}"))
                .andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/operating-expenses/statistics").header("Authorization", owner).param("keyword", key))
                .andExpect(jsonPath("$.data.totalAmount").value(26.21)).andExpect(jsonPath("$.data.totalCount").value(2));
        mvc.perform(post("/operating-expenses").header("Authorization", owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"invalid\",\"amount\":0.001}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/operating-expenses").header("Authorization", other)).andExpect(jsonPath("$.code").value(403));
        mvc.perform(delete("/operating-expenses/{id}", id).header("Authorization", owner)).andExpect(jsonPath("$.code").value(200));
        mvc.perform(delete("/operating-expenses/{id}", id).header("Authorization", owner)).andExpect(jsonPath("$.code").value(404));
    }
}
