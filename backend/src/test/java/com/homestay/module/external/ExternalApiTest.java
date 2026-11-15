package com.homestay.module.external;

import com.homestay.common.exception.GlobalExceptionHandler;
import com.homestay.context.UserContext;
import com.homestay.context.UserContextHolder;
import com.homestay.module.external.controller.ExternalController;
import com.homestay.module.external.service.ExternalService;
import com.homestay.module.external.vo.ExternalCatalogVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 第三方平台 HTTP 接口的独立切片测试，不连接真实数据库。 */
class ExternalApiTest {
    private ExternalService service;
    private MockMvc mvc;

    @BeforeEach void setup() {
        service = mock(ExternalService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ExternalController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        UserContext user = new UserContext(); user.setUserType("employee"); user.setRole("owner");
        UserContextHolder.setContext(user);
    }
    @AfterEach void cleanup() { UserContextHolder.clear(); }

    @Test void catalogEndpointsMapInputsAndOutput() throws Exception {
        when(service.platforms(true)).thenReturn(List.of(new ExternalCatalogVO(1L, "抖音", 1, null)));
        when(service.savePlatform(any(), any())).thenReturn(1L);
        when(service.saveAnchor(any(), any())).thenReturn(2L);
        mvc.perform(get("/external/platforms").param("includeDisabled", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("抖音"));
        mvc.perform(get("/external/anchors")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/external/platforms").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"抖音\",\"status\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(1));
        mvc.perform(put("/external/platforms/1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"美团\",\"status\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/external/anchors").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"主播\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(2));
        mvc.perform(put("/external/anchors/2").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"主播\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).savePlatform(eq(1L), any()); verify(service).saveAnchor(eq(2L), any());
    }

    @Test void orderAndReportEndpointsValidateAndDispatch() throws Exception {
        String order = "{\"platformId\":1,\"roomId\":2,\"checkInDate\":\"2026-05-01\",\"checkOutDate\":\"2026-05-02\",\"guestCount\":1,\"totalAmount\":100,\"platformFee\":5,\"anchorFee\":0}";
        when(service.createOrder(any())).thenReturn(7L);
        mvc.perform(post("/external/orders").contentType(MediaType.APPLICATION_JSON).content(order))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(7));
        mvc.perform(put("/external/orders/7").contentType(MediaType.APPLICATION_JSON).content(order))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(post("/external/orders/7/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CHECKED_IN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/external/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/external/orders/7")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/external/calendar").param("startDate", "2026-05-01").param("endDate", "2026-05-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mvc.perform(get("/external/statistics").param("startDate", "2026-05-01").param("endDate", "2026-05-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).changeStatus(7L, "CHECKED_IN");
        mvc.perform(post("/external/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/external/orders").contentType(MediaType.APPLICATION_JSON).content(order.replace("100,", "100.001,")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/external/orders").contentType(MediaType.APPLICATION_JSON).content(order.replace("\"guestCount\":1", "\"guestCount\":0")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }

    @Test void missingInvalidParametersAndMalformedJsonAreClientErrors() throws Exception {
        mvc.perform(get("/external/calendar").param("startDate", "wrong").param("endDate", "2026-05-31"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(get("/external/calendar").param("startDate", "2026-05-01"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/external/orders").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(service);
    }

    @Test void internalFailureDoesNotExposeDetails() throws Exception {
        when(service.platforms(false)).thenThrow(new IllegalStateException("internal-detail"));
        mvc.perform(get("/external/platforms")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500)).andExpect(jsonPath("$.message", not(containsString("internal-detail"))));
    }
}
