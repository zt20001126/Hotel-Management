package com.homestay.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilTest {
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-at-least-32-bytes-long");
    }

    @Test
    void guestTokenExpiresAfterThirtyDays() {
        assertTokenLifetime("guest", 30);
    }

    @Test
    void employeeTokenExpiresAfterOneDay() {
        assertTokenLifetime("employee", 1);
    }

    private void assertTokenLifetime(String userType, long days) {
        Claims claims = jwtUtil.parseToken(jwtUtil.generateToken(1L, "user", userType, userType));
        assertEquals(TimeUnit.DAYS.toMillis(days),
                claims.getExpiration().getTime() - claims.getIssuedAt().getTime());
    }
}
