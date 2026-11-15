package com.homestay.module.external.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NightlyAllocationTest {
    @Test
    void distributesRemainderInDateOrderAndPreservesTotal() {
        List<BigDecimal> result = NightlyAllocation.allocate(new BigDecimal("1000.00"), 3);
        assertEquals(List.of(new BigDecimal("333.34"), new BigDecimal("333.33"),
                new BigDecimal("333.33")), result);
        assertEquals(new BigDecimal("1000.00"), result.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void allocatesOneCentAndRejectsInvalidAmounts() {
        assertEquals(List.of(new BigDecimal("0.01"), new BigDecimal("0.00")),
                NightlyAllocation.allocate(new BigDecimal("0.01"), 2));
        assertThrows(IllegalArgumentException.class, () -> NightlyAllocation.allocate(BigDecimal.ONE, 0));
        assertThrows(ArithmeticException.class, () -> NightlyAllocation.allocate(new BigDecimal("0.001"), 2));
    }
}
