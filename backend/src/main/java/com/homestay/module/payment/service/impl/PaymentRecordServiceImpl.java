package com.homestay.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homestay.common.exception.BusinessException;
import com.homestay.context.UserContextHolder;
import com.homestay.module.order.entity.Order;
import com.homestay.module.order.mapper.OrderMapper;
import com.homestay.module.payment.entity.PaymentRecord;
import com.homestay.module.payment.mapper.PaymentRecordMapper;
import com.homestay.module.payment.service.PaymentRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements PaymentRecordService {

    @Autowired
    private OrderMapper orderMapper;

    @Override
    @Transactional
    public PaymentRecord createPayment(PaymentRecord payment) {
        Order order = orderMapper.selectForPayment(payment.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"guest".equals(UserContextHolder.getUserType())
                || !java.util.Objects.equals(order.getGuestId(), UserContextHolder.getUserId())) {
            throw new BusinessException("只能支付自己的预订");
        }
        if (!"awaiting_payment".equals(order.getStatus()) || order.getTotalPrice() == null
                || order.getTotalPrice().signum() <= 0 || payment.getAmount() == null
                || payment.getAmount().compareTo(order.getTotalPrice()) != 0
                || !"room_fee".equals(payment.getPaymentType())
                || !"mock".equals(payment.getPaymentMethod())) {
            throw new BusinessException("仅支持一次模拟支付全部房费");
        }
        
        payment.setPaymentNo(generatePaymentNo());
        payment.setOrderNo(order.getOrderNo());
        payment.setGuestId(order.getGuestId());
        payment.setStatus("success");
        payment.setOperatorId(UserContextHolder.getUserId());
        
        if (!this.save(payment)) {
            throw new BusinessException("模拟支付记录保存失败");
        }
        
        order.setPaidAmount(order.getPaidAmount().add(payment.getAmount()));
        order.setStatus("pending");
        if (orderMapper.updateById(order) != 1) {
            throw new BusinessException("支付状态更新失败");
        }
        
        return payment;
    }

    private String generatePaymentNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = this.count(new LambdaQueryWrapper<PaymentRecord>()
                .likeRight(PaymentRecord::getPaymentNo, "PAY" + date));
        return String.format("PAY%s%04d", date, count + 1);
    }

    @Override
    public IPage<PaymentRecord> getPaymentPage(Integer page, Integer size, String orderNo, String paymentType) {
        Page<PaymentRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(orderNo)) {
            wrapper.like(PaymentRecord::getOrderNo, orderNo);
        }
        
        if (StringUtils.hasText(paymentType)) {
            wrapper.eq(PaymentRecord::getPaymentType, paymentType);
        }
        
        wrapper.orderByDesc(PaymentRecord::getCreateTime);
        return this.page(pageParam, wrapper);
    }

    @Override
    public List<PaymentRecord> getOrderPayments(Long orderId) {
        return this.list(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .orderByDesc(PaymentRecord::getCreateTime));
    }

    @Override
    public BigDecimal getOrderPaidAmount(Long orderId) {
        List<PaymentRecord> payments = this.list(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .eq(PaymentRecord::getStatus, "success")
                .ne(PaymentRecord::getPaymentType, "refund"));
        
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentRecord payment : payments) {
            total = total.add(payment.getAmount());
        }
        
        List<PaymentRecord> refunds = this.list(new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getOrderId, orderId)
                .eq(PaymentRecord::getPaymentType, "refund")
                .eq(PaymentRecord::getStatus, "success"));
        
        for (PaymentRecord refund : refunds) {
            total = total.subtract(refund.getAmount());
        }
        
        return total;
    }

    @Override
    @Transactional
    public PaymentRecord createRefund(Long orderId, BigDecimal amount, String remark) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        BigDecimal paidAmount = getOrderPaidAmount(orderId);
        if (amount.compareTo(paidAmount) > 0) {
            throw new BusinessException("退款金额不能超过已付金额");
        }
        
        PaymentRecord refund = new PaymentRecord();
        refund.setPaymentNo(generatePaymentNo());
        refund.setOrderId(orderId);
        refund.setOrderNo(order.getOrderNo());
        refund.setGuestId(order.getGuestId());
        refund.setAmount(amount);
        refund.setPaymentType("refund");
        refund.setStatus("success");
        refund.setRemark(remark);
        refund.setOperatorId(UserContextHolder.getUserId());
        
        this.save(refund);
        
        order.setPaidAmount(order.getPaidAmount().subtract(amount));
        orderMapper.updateById(order);
        
        return refund;
    }
}
