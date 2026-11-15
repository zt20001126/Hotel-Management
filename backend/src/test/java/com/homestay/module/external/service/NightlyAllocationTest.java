package com.homestay.module.external.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

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

    /** 覆盖每一种允许的住宿晚数，以及固定种子的金额样本和最大金额。 */
    @Test
    void conservesCentsAcrossAllStayLengthsAndBoundaryAmounts() {
        long[] amounts = {0, 1, 2, 99, 100, 101, 9999, 100000, 999999999999L};
        for (int nights = 1; nights <= 366; nights++) {
            for (long cents : amounts) verifyAllocation(cents, nights);
        }
        Random random = new Random(20261115L);
        for (int sample = 0; sample < 5000; sample++) {
            verifyAllocation(random.nextLong(1000000000000L), random.nextInt(366) + 1);
        }
        assertThrows(IllegalArgumentException.class, () -> NightlyAllocation.allocate(null, 1));
        assertThrows(IllegalArgumentException.class, () -> NightlyAllocation.allocate(new BigDecimal("-0.01"), 1));
    }

    private static void verifyAllocation(long cents, int nights) {
        List<BigDecimal> result = NightlyAllocation.allocate(BigDecimal.valueOf(cents, 2), nights);
        assertEquals(nights, result.size());
        long sum = 0;
        for (int i = 0; i < nights; i++) {
            long actual = result.get(i).movePointRight(2).longValueExact();
            assertEquals(cents / nights + (i < cents % nights ? 1 : 0), actual);
            sum += actual;
        }
        assertEquals(cents, sum);
    }
}
