package com.homestay.module.external;

import com.homestay.common.exception.BusinessException;
import com.homestay.module.external.dto.ExternalOrderRequest;
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
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** 第三方订单增改、取消和房态边界的 MySQL 集成测试。 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "EXTERNAL_INTEGRATION_TEST", matches = "true")
class ExternalOrderLifecycleIntegrationTest {
    @Autowired private ExternalService service;
    @Autowired private JdbcTemplate jdbc;

    private long room(int index) {
        return jdbc.queryForObject("SELECT id FROM room WHERE deleted=0 ORDER BY id LIMIT 1 OFFSET " + index, Long.class);
    }
    private long platform() { return jdbc.queryForObject("SELECT id FROM external_platform WHERE status=1 ORDER BY id LIMIT 1", Long.class); }
    private ExternalOrderRequest request(long room, long platform, String start, String end) {
        ExternalOrderRequest request = new ExternalOrderRequest();
        request.setRoomId(room); request.setPlatformId(platform); request.setCheckInDate(LocalDate.parse(start));
        request.setCheckOutDate(LocalDate.parse(end)); request.setGuestCount(2); request.setTotalAmount(new BigDecimal("100.00"));
        request.setPlatformFee(new BigDecimal("10.00")); request.setAnchorFee(BigDecimal.ZERO);
        request.setPlatformOrderNo(UUID.randomUUID().toString());
        return request;
    }
    private void amount(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual)); }

    @Test void concurrentMovesToSameRoomAllowOnlyOne() throws Exception {
        long source1 = room(0), source2 = room(1), target = room(2), platform = platform();
        Long first = service.createOrder(request(source1, platform, "2064-06-01", "2064-06-03"));
        Long second = service.createOrder(request(source2, platform, "2064-06-01", "2064-06-03"));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<String> change1 = () -> moveAfterSignal(start, first, request(target, platform, "2064-06-01", "2064-06-03"));
            Callable<String> change2 = () -> moveAfterSignal(start, second, request(target, platform, "2064-06-01", "2064-06-03"));
            Future<String> a = executor.submit(change1), b = executor.submit(change2);
            start.countDown();
            String result1 = a.get(20, TimeUnit.SECONDS), result2 = b.get(20, TimeUnit.SECONDS);
            assertTrue((result1.equals("OK") && result2.contains("已有第三方平台订单")) ||
                    (result2.equals("OK") && result1.contains("已有第三方平台订单")));
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM external_order WHERE id IN (?,?) AND room_id=?", Integer.class, first, second, target));
        } finally {
            executor.shutdownNow();
            jdbc.update("DELETE FROM external_order WHERE id IN (?,?)", first, second);
        }
    }
    private String moveAfterSignal(CountDownLatch signal, Long id, ExternalOrderRequest request) throws Exception {
        signal.await();
        try { service.updateOrder(id, request); return "OK"; }
        catch (BusinessException ex) { return ex.getMessage(); }
    }

    @Test @Transactional void rejectsEveryOverlapAndAllowsAdjacentDates() {
        long room = room(0), platform = platform();
        service.createOrder(request(room, platform, "2060-01-10", "2060-01-15"));
        String[][] overlaps = {{"2060-01-05", "2060-01-11"}, {"2060-01-10", "2060-01-15"},
                {"2060-01-11", "2060-01-12"}, {"2060-01-14", "2060-01-16"}, {"2060-01-05", "2060-01-20"}};
        for (String[] dates : overlaps) {
            BusinessException ex = assertThrows(BusinessException.class, () -> service.createOrder(request(room, platform, dates[0], dates[1])));
            assertTrue(ex.getMessage().contains("已有第三方平台订单"));
        }
        assertNotNull(service.createOrder(request(room, platform, "2060-01-05", "2060-01-10")));
        assertNotNull(service.createOrder(request(room, platform, "2060-01-15", "2060-01-20")));
    }

    @Test @Transactional void platformOrderNumbersAreUniquePerPlatformAndOptional() {
        long r1 = room(0), r2 = room(1), p1 = platform();
        long p2 = jdbc.queryForObject("SELECT id FROM external_platform WHERE status=1 ORDER BY id LIMIT 1 OFFSET 1", Long.class);
        ExternalOrderRequest first = request(r1, p1, "2066-01-01", "2066-01-02");
        first.setPlatformOrderNo("  " + UUID.randomUUID() + "  ");
        Long firstId = service.createOrder(first);
        String normalized = service.order(firstId).platformOrderNo();
        assertEquals(normalized.trim(), normalized);
        ExternalOrderRequest sameNumberDifferentPlatform = request(r2, p2, "2066-01-01", "2066-01-02");
        sameNumberDifferentPlatform.setPlatformOrderNo(normalized);
        assertNotNull(service.createOrder(sameNumberDifferentPlatform));
        ExternalOrderRequest duplicate = request(r2, p1, "2066-01-02", "2066-01-03");
        duplicate.setPlatformOrderNo(normalized);
        assertThrows(BusinessException.class, () -> service.createOrder(duplicate));
        ExternalOrderRequest blank1 = request(r1, p1, "2066-01-02", "2066-01-03"); blank1.setPlatformOrderNo("  ");
        ExternalOrderRequest blank2 = request(r1, p1, "2066-01-03", "2066-01-04"); blank2.setPlatformOrderNo(null);
        assertNull(service.order(service.createOrder(blank1)).platformOrderNo());
        assertNull(service.order(service.createOrder(blank2)).platformOrderNo());
    }

    @Test @Transactional void modifyPreservesOriginalWhenConflictOrDuplicateAndReleasesOldDates() {
        long r1 = room(0), r2 = room(1), platform = platform();
        ExternalOrderRequest original = request(r1, platform, "2061-02-01", "2061-02-03");
        original.setRemark("原备注");
        Long id = service.createOrder(original);
        ExternalOrderRequest occupied = request(r2, platform, "2061-02-04", "2061-02-06");
        service.createOrder(occupied);
        ExternalOrderRequest conflict = request(r2, platform, "2061-02-05", "2061-02-06");
        assertThrows(BusinessException.class, () -> service.updateOrder(id, conflict));
        assertEquals(r1, service.order(id).roomId());
        assertEquals(LocalDate.of(2061, 2, 1), service.order(id).checkInDate());
        ExternalOrderRequest duplicate = request(r2, platform, "2061-02-08", "2061-02-09");
        duplicate.setPlatformOrderNo(occupied.getPlatformOrderNo());
        assertThrows(BusinessException.class, () -> service.updateOrder(id, duplicate));
        assertEquals(r1, service.order(id).roomId());
        ExternalOrderRequest updated = request(r2, platform, "2061-02-06", "2061-02-08");
        updated.setTotalAmount(new BigDecimal("200.01")); updated.setPlatformFee(new BigDecimal("20.02"));
        updated.setPlatformOrderNo("  "); updated.setRemark(null);
        service.updateOrder(id, updated);
        ExternalOrderVO saved = service.order(id);
        assertEquals(r2, saved.roomId()); assertNull(saved.platformOrderNo()); assertNull(saved.remark());
        amount("179.99", saved.netProfit());
        assertNotNull(service.createOrder(request(r1, platform, "2061-02-01", "2061-02-03")));
        assertThrows(BusinessException.class, () -> service.createOrder(request(r2, platform, "2061-02-07", "2061-02-08")));
    }

    /** 记录目前两个编辑页面基于旧数据保存时，后保存覆盖先前修改的风险。 */
    @Test @Transactional void staleEditsCurrentlyOverwriteEarlierChanges() {
        long room = room(0), platform = platform();
        ExternalOrderRequest initial = request(room, platform, "2067-01-01", "2067-01-02");
        Long id = service.createOrder(initial);
        ExternalOrderRequest editorOne = request(room, platform, "2067-01-01", "2067-01-02");
        ExternalOrderRequest staleEditorTwo = request(room, platform, "2067-01-01", "2067-01-02");
        editorOne.setPlatformOrderNo(initial.getPlatformOrderNo());
        staleEditorTwo.setPlatformOrderNo(initial.getPlatformOrderNo());
        editorOne.setPlatformFee(new BigDecimal("15.00"));
        staleEditorTwo.setRemark("第二位修改备注");
        service.updateOrder(id, editorOne);
        amount("15.00", service.order(id).platformFee());
        service.updateOrder(id, staleEditorTwo);
        amount("10.00", service.order(id).platformFee());
    }

    @Test @Transactional void cancellationAndStatusTransitionsKeepCalendarAndOrdersConsistent() {
        long room = room(0), platform = platform();
        LocalDate start = LocalDate.of(2062, 3, 1), end = LocalDate.of(2062, 3, 2);
        Long id = service.createOrder(request(room, platform, "2062-03-01", "2062-03-03"));
        assertEquals(1, service.calendar(start, end, null).size());
        service.changeStatus(id, "CANCELLED");
        assertTrue(service.calendar(start, end, null).isEmpty());
        assertEquals("CANCELLED", service.order(id).status());
        assertThrows(BusinessException.class, () -> service.changeStatus(id, "CANCELLED"));
        assertThrows(BusinessException.class, () -> service.updateOrder(id, request(room, platform, "2062-03-01", "2062-03-03")));
        Long replacement = service.createOrder(request(room, platform, "2062-03-01", "2062-03-03"));
        assertThrows(BusinessException.class, () -> service.changeStatus(replacement, "CHECKED_OUT"));
        service.changeStatus(replacement, "CHECKED_IN");
        assertThrows(BusinessException.class, () -> service.changeStatus(replacement, "CANCELLED"));
        assertThrows(BusinessException.class, () -> service.updateOrder(replacement, request(room, platform, "2062-03-01", "2062-03-03")));
        service.changeStatus(replacement, "CHECKED_OUT");
        assertThrows(BusinessException.class, () -> service.createOrder(request(room, platform, "2062-03-02", "2062-03-03")));
        assertEquals(1, service.calendar(start, end, null).size());
    }

    @Test @Transactional void invalidDatesNumbersAndInactivePlatformHaveNoSideEffects() {
        long room = room(0), platform = platform();
        assertThrows(BusinessException.class, () -> service.createOrder(request(room, platform, "2063-04-02", "2063-04-02")));
        assertThrows(BusinessException.class, () -> service.createOrder(request(room, platform, "2063-04-03", "2063-04-02")));
        assertThrows(BusinessException.class, () -> service.createOrder(request(room, platform, "2063-01-01", "2064-01-03")));
        ExternalOrderRequest nonzeroCommission = request(room, platform, "2063-04-01", "2063-04-02");
        nonzeroCommission.setAnchorFee(new BigDecimal("0.01"));
        assertThrows(BusinessException.class, () -> service.createOrder(nonzeroCommission));
        Long id = service.createOrder(request(room, platform, "2063-04-01", "2063-04-02"));
        jdbc.update("UPDATE external_platform SET status=0 WHERE id=?", platform);
        assertThrows(BusinessException.class, () -> service.createOrder(request(room, platform, "2063-04-02", "2063-04-03")));
        ExternalOrderRequest samePlatform = request(room, platform, "2063-04-01", "2063-04-02");
        service.updateOrder(id, samePlatform);
        assertEquals(platform, service.order(id).platformId());
    }
}
