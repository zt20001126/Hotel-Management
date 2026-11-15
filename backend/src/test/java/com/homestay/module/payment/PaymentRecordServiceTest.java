package com.homestay.module.payment;

import com.homestay.common.exception.BusinessException;
import com.homestay.context.UserContext;
import com.homestay.context.UserContextHolder;
import com.homestay.module.order.entity.Order;
import com.homestay.module.order.mapper.OrderMapper;
import com.homestay.module.payment.entity.PaymentRecord;
import com.homestay.module.payment.service.impl.PaymentRecordServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentRecordServiceTest {
    private PaymentRecordServiceImpl service;
    private OrderMapper mapper;
    private Order order;

    @BeforeEach
    void setUp() {
        service = spy(new PaymentRecordServiceImpl());
        mapper = mock(OrderMapper.class);
        ReflectionTestUtils.setField(service, "orderMapper", mapper);
        UserContext context = new UserContext();
        context.setUserId(7L);
        context.setUserType("guest");
        UserContextHolder.setContext(context);
        order = new Order();
        order.setId(12L);
        order.setGuestId(7L);
        order.setOrderNo("ORD-TEST");
        order.setStatus("awaiting_payment");
        order.setTotalPrice(new BigDecimal("100.00"));
        order.setPaidAmount(BigDecimal.ZERO);
        when(mapper.selectForPayment(12L)).thenReturn(order);
    }

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
    }

    private PaymentRecord payment(String amount) {
        PaymentRecord payment = new PaymentRecord();
        payment.setOrderId(12L);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentType("room_fee");
        payment.setPaymentMethod("mock");
        return payment;
    }

    @Test
    void rejectsPartialAndDuplicatePayment() {
        assertThrows(BusinessException.class, () -> service.createPayment(payment("30.00")));
        order.setStatus("pending");
        assertThrows(BusinessException.class, () -> service.createPayment(payment("100.00")));
        verify(mapper, never()).updateById(any(Order.class));
    }

    @Test
    void rejectsPaymentForAnotherGuest() {
        order.setGuestId(8L);
        assertThrows(BusinessException.class, () -> service.createPayment(payment("100.00")));
        verify(mapper, never()).updateById(any(Order.class));
    }

    @Test
    void fullMockPaymentMakesOrderPendingConfirmation() {
        doReturn(0L).when(service).count(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        doReturn(true).when(service).save(any(PaymentRecord.class));
        when(mapper.updateById(order)).thenReturn(1);

        PaymentRecord result = service.createPayment(payment("100.00"));

        assertEquals("success", result.getStatus());
        assertEquals("pending", order.getStatus());
        assertEquals(0, order.getPaidAmount().compareTo(order.getTotalPrice()));
    }
}
