package com.homestay.module.order;

import com.homestay.common.exception.BusinessException;
import com.homestay.module.order.entity.Order;
import com.homestay.module.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

class OrderPaymentGateTest {
    @Test
    void unpaidOrderCannotBeConfirmedOrCheckedIn() {
        OrderServiceImpl service = spy(new OrderServiceImpl());
        Order order = new Order();
        order.setId(1L);
        order.setStatus("pending");
        order.setTotalPrice(new BigDecimal("100.00"));
        order.setPaidAmount(new BigDecimal("30.00"));
        doReturn(order).when(service).getById(1L);

        assertThrows(BusinessException.class, () -> service.confirmOrder(1L));
        assertThrows(BusinessException.class, () -> service.checkIn(1L, 1L));
        assertThrows(BusinessException.class, () -> service.getOrderDetail(1L));
    }
}
