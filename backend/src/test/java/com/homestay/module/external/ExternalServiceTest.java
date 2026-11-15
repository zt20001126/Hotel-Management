package com.homestay.module.external;

import com.homestay.module.external.entity.*;
import com.homestay.module.external.mapper.*;
import com.homestay.module.external.service.impl.ExternalServiceImpl;
import com.homestay.module.external.vo.ExternalStatisticsVO;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 验证跨月统计按房晚分摊且订单按区间去重。 */
class ExternalServiceTest {
    @Test void allocatesCrossMonthAndGroupsByOrder() {
        ExternalPlatformMapper platforms = mock(ExternalPlatformMapper.class);
        ExternalAnchorMapper anchors = mock(ExternalAnchorMapper.class);
        ExternalOrderMapper orders = mock(ExternalOrderMapper.class);
        ExternalRoomMapper rooms = mock(ExternalRoomMapper.class);
        ExternalOrder order = new ExternalOrder();
        order.setId(1L); order.setPlatformId(2L); order.setRoomId(3L);
        order.setCheckInDate(LocalDate.of(2026, 5, 30)); order.setCheckOutDate(LocalDate.of(2026, 6, 2));
        order.setTotalAmount(new BigDecimal("1000.00")); order.setPlatformFee(new BigDecimal("100.00"));
        order.setAnchorFee(BigDecimal.ZERO); order.setStatus("CHECKED_IN");
        ExternalPlatform platform = new ExternalPlatform(); platform.setId(2L); platform.setName("抖音");
        when(platforms.selectById(2L)).thenReturn(platform); when(rooms.findRoomNumber(3L)).thenReturn("101");
        when(orders.selectList(any())).thenReturn(List.of(order));
        ExternalServiceImpl service = new ExternalServiceImpl(platforms, anchors, orders, rooms);
        ExternalStatisticsVO report = service.statistics(LocalDate.of(2026,5,1), LocalDate.of(2026,5,31), null, null, null);
        assertEquals(1, report.total().orderCount()); assertEquals(2, report.total().nights());
        assertEquals(new BigDecimal("666.67"), report.total().totalAmount());
        assertEquals(new BigDecimal("66.67"), report.total().platformFee());
        assertEquals(new BigDecimal("600.00"), report.total().netProfit());
        assertEquals("抖音", report.platforms().get(0).name());
        assertEquals("无主播", report.anchors().get(0).name());
    }
}
