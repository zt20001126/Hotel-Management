package com.homestay.module.external.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.exception.BusinessException;
import com.homestay.common.result.PageResult;
import com.homestay.module.external.dto.*;
import com.homestay.module.external.entity.*;
import com.homestay.module.external.enums.ExternalOrderStatus;
import com.homestay.module.external.mapper.*;
import com.homestay.module.external.service.ExternalService;
import com.homestay.module.external.service.NightlyAllocation;
import com.homestay.module.external.vo.*;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 第三方平台经营服务；不操作小程序订单和支付数据。 */
@Service
public class ExternalServiceImpl implements ExternalService {
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;
    private static final int MAX_PAGE_SIZE = 100;
    private static final long MAX_QUERY_DAYS = 366;
    private static final long MAX_STAY_DAYS = 366;
    private final ExternalPlatformMapper platforms;
    private final ExternalAnchorMapper anchors;
    private final ExternalOrderMapper orders;
    private final ExternalRoomMapper rooms;

    public ExternalServiceImpl(ExternalPlatformMapper platforms, ExternalAnchorMapper anchors,
                               ExternalOrderMapper orders, ExternalRoomMapper rooms) {
        this.platforms = platforms; this.anchors = anchors; this.orders = orders; this.rooms = rooms;
    }

    public List<ExternalCatalogVO> platforms(boolean all) {
        return platforms.selectList(new LambdaQueryWrapper<ExternalPlatform>().eq(!all, ExternalPlatform::getStatus, ENABLED)
                .orderByAsc(ExternalPlatform::getId)).stream().map(p -> new ExternalCatalogVO(p.getId(), p.getName(), p.getStatus(), null)).toList();
    }
    public List<ExternalCatalogVO> anchors(boolean all) {
        return anchors.selectList(new LambdaQueryWrapper<ExternalAnchor>().eq(!all, ExternalAnchor::getStatus, ENABLED)
                .orderByAsc(ExternalAnchor::getId)).stream().map(a -> new ExternalCatalogVO(a.getId(), a.getName(), a.getStatus(), a.getRemark())).toList();
    }
    private void validStatus(Integer value) {
        if (value == null || (value != ENABLED && value != DISABLED)) throw new BusinessException(400, "启用状态无效");
    }
    @Transactional
    public Long savePlatform(Long id, ExternalCatalogRequest request) {
        validStatus(request.getStatus());
        ExternalPlatform p = id == null ? new ExternalPlatform() : platforms.selectById(id);
        if (p == null) throw new BusinessException(404, "平台不存在");
        p.setName(request.getName().trim()); p.setStatus(request.getStatus());
        try { if (id == null) platforms.insert(p); else platforms.updateById(p); }
        catch (DuplicateKeyException e) { throw new BusinessException(400, "平台名称已存在"); }
        return p.getId();
    }
    @Transactional
    public Long saveAnchor(Long id, ExternalCatalogRequest request) {
        validStatus(request.getStatus());
        ExternalAnchor a = id == null ? new ExternalAnchor() : anchors.selectById(id);
        if (a == null) throw new BusinessException(404, "主播不存在");
        a.setName(request.getName().trim()); a.setStatus(request.getStatus()); a.setRemark(request.getRemark());
        if (id == null) {
            anchors.insert(a);
        } else {
            // 显式写入空备注，避免默认的非空更新策略保留旧值。
            anchors.update(null, new LambdaUpdateWrapper<ExternalAnchor>()
                    .eq(ExternalAnchor::getId, id).set(ExternalAnchor::getName, a.getName())
                    .set(ExternalAnchor::getStatus, a.getStatus()).set(ExternalAnchor::getRemark, a.getRemark()));
        }
        return a.getId();
    }
    private ExternalOrder existing(Long id) {
        ExternalOrder order = orders.selectById(id);
        if (order == null) throw new BusinessException(404, "订单不存在");
        return order;
    }
    private void validate(ExternalOrderRequest r, ExternalOrder original) {
        if (!r.getCheckInDate().isBefore(r.getCheckOutDate()) || ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate()) > MAX_STAY_DAYS)
            throw new BusinessException(400, "住宿日期无效或住宿时间过长");
        if (r.getAnchorId() == null && r.getAnchorFee().signum() != 0) throw new BusinessException(400, "未选择主播时主播手续费必须为零");
        ExternalPlatform platform = platforms.selectById(r.getPlatformId());
        if (platform == null || (!Objects.equals(platform.getStatus(), ENABLED) && (original == null || !Objects.equals(original.getPlatformId(), platform.getId()))))
            throw new BusinessException(400, "平台不存在或已停用");
        if (r.getAnchorId() != null) {
            ExternalAnchor anchor = anchors.selectById(r.getAnchorId());
            if (anchor == null || (!Objects.equals(anchor.getStatus(), ENABLED) && (original == null || !Objects.equals(original.getAnchorId(), anchor.getId()))))
                throw new BusinessException(400, "主播不存在或已停用");
        }
        // 将同一房间的第三方订单写入串行化，避免并发检查后同时插入。
        if (orders.lockRoom(r.getRoomId()) == null) throw new BusinessException(400, "房间不存在");
        if (orders.countConflicts(r.getRoomId(), r.getCheckInDate(), r.getCheckOutDate(), original == null ? null : original.getId()) > 0)
            throw new BusinessException(400, "该房间在所选日期已有第三方平台订单");
    }
    private void copy(ExternalOrderRequest request, ExternalOrder order) {
        BeanUtils.copyProperties(request, order);
        order.setPlatformOrderNo(StringUtils.hasText(request.getPlatformOrderNo()) ? request.getPlatformOrderNo().trim() : null);
    }
    /** Step 1: 锁定房间并校验；Step 2: 保存独立的第三方订单。 */
    @Transactional
    public Long createOrder(ExternalOrderRequest request) {
        validate(request, null);
        ExternalOrder order = new ExternalOrder(); copy(request, order);
        order.setOrderNo("EXT" + UUID.randomUUID().toString().replace("-", ""));
        order.setStatus(ExternalOrderStatus.BOOKED.name());
        try { orders.insert(order); } catch (DuplicateKeyException e) { throw new BusinessException(400, "平台订单号已存在"); }
        return order.getId();
    }
    @Transactional
    public void updateOrder(Long id, ExternalOrderRequest request) {
        ExternalOrder old = existing(id);
        if (!ExternalOrderStatus.BOOKED.name().equals(old.getStatus())) throw new BusinessException(400, "仅已预订订单允许修改");
        // 按房间 ID 排序持锁，防止两笔换房操作发生死锁。
        if (old.getRoomId().compareTo(request.getRoomId()) < 0) orders.lockRoom(old.getRoomId());
        validate(request, old);
        if (old.getRoomId().compareTo(request.getRoomId()) > 0) orders.lockRoom(old.getRoomId());
        try {
            int changed = orders.update(null, new LambdaUpdateWrapper<ExternalOrder>().eq(ExternalOrder::getId, id)
                    .eq(ExternalOrder::getStatus, ExternalOrderStatus.BOOKED.name())
                    .set(ExternalOrder::getPlatformOrderNo, StringUtils.hasText(request.getPlatformOrderNo()) ? request.getPlatformOrderNo().trim() : null)
                    .set(ExternalOrder::getPlatformId, request.getPlatformId()).set(ExternalOrder::getAnchorId, request.getAnchorId())
                    .set(ExternalOrder::getRoomId, request.getRoomId()).set(ExternalOrder::getCheckInDate, request.getCheckInDate())
                    .set(ExternalOrder::getCheckOutDate, request.getCheckOutDate()).set(ExternalOrder::getGuestCount, request.getGuestCount())
                    .set(ExternalOrder::getTotalAmount, request.getTotalAmount()).set(ExternalOrder::getPlatformFee, request.getPlatformFee())
                    .set(ExternalOrder::getAnchorFee, request.getAnchorFee()).set(ExternalOrder::getRemark, request.getRemark()));
            if (changed == 0) throw new BusinessException(400, "订单已变更，请刷新后重试");
        } catch (DuplicateKeyException e) { throw new BusinessException(400, "平台订单号已存在"); }
    }
    @Transactional
    public void changeStatus(Long id, String target) {
        ExternalOrder order = existing(id);
        boolean allowed = (ExternalOrderStatus.BOOKED.name().equals(order.getStatus()) &&
                (ExternalOrderStatus.CHECKED_IN.name().equals(target) || ExternalOrderStatus.CANCELLED.name().equals(target))) ||
                (ExternalOrderStatus.CHECKED_IN.name().equals(order.getStatus()) && ExternalOrderStatus.CHECKED_OUT.name().equals(target));
        if (!allowed) throw new BusinessException(400, "不允许的订单状态变更");
        int changed = orders.update(null, new LambdaUpdateWrapper<ExternalOrder>().eq(ExternalOrder::getId, id)
                .eq(ExternalOrder::getStatus, order.getStatus()).set(ExternalOrder::getStatus, target));
        if (changed == 0) throw new BusinessException(400, "订单已变更，请刷新后重试");
    }
    private ExternalOrderVO view(ExternalOrder o) {
        ExternalPlatform platform = platforms.selectById(o.getPlatformId());
        ExternalAnchor anchor = o.getAnchorId() == null ? null : anchors.selectById(o.getAnchorId());
        String roomName = rooms.findRoomNumber(o.getRoomId());
        return new ExternalOrderVO(o.getId(), o.getOrderNo(), o.getPlatformOrderNo(), o.getPlatformId(),
                platform == null ? "已删除平台" : platform.getName(), o.getAnchorId(), anchor == null ? "无主播" : anchor.getName(),
                o.getRoomId(), roomName == null ? "已删除房间（" + o.getRoomId() + "）" : roomName,
                o.getCheckInDate(), o.getCheckOutDate(), ChronoUnit.DAYS.between(o.getCheckInDate(), o.getCheckOutDate()),
                o.getGuestCount(), o.getTotalAmount(), o.getPlatformFee(), o.getAnchorFee(),
                o.getTotalAmount().subtract(o.getPlatformFee()).subtract(o.getAnchorFee()), o.getStatus(), o.getRemark());
    }
    public ExternalOrderVO order(Long id) { return view(existing(id)); }
    private void range(LocalDate start, LocalDate end) {
        if (start == null || end == null || end.isBefore(start) || ChronoUnit.DAYS.between(start, end) >= MAX_QUERY_DAYS)
            throw new BusinessException(400, "日期范围无效或超过一年");
    }
    private LambdaQueryWrapper<ExternalOrder> filter(LocalDate start, LocalDate end, Long platform, Long anchor, Long room) {
        return new LambdaQueryWrapper<ExternalOrder>().lt(start != null && end != null, ExternalOrder::getCheckInDate, end == null ? null : end.plusDays(1))
                .gt(start != null && end != null, ExternalOrder::getCheckOutDate, start)
                .eq(platform != null, ExternalOrder::getPlatformId, platform).eq(anchor != null, ExternalOrder::getAnchorId, anchor)
                .eq(room != null, ExternalOrder::getRoomId, room);
    }
    public PageResult<ExternalOrderVO> orders(int page, int size, LocalDate start, LocalDate end, Long platform, Long anchor, Long room, String status, String keyword) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) throw new BusinessException(400, "分页参数无效");
        if ((start == null) != (end == null)) throw new BusinessException(400, "请同时填写开始和结束日期");
        if (start != null) range(start, end);
        LambdaQueryWrapper<ExternalOrder> query = filter(start, end, platform, anchor, room)
                .eq(StringUtils.hasText(status), ExternalOrder::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(ExternalOrder::getOrderNo, keyword).or().like(ExternalOrder::getPlatformOrderNo, keyword))
                .orderByDesc(ExternalOrder::getId);
        Page<ExternalOrder> result = orders.selectPage(new Page<>(page, size), query);
        Page<ExternalOrderVO> mapped = new Page<>(page, size, result.getTotal());
        mapped.setRecords(result.getRecords().stream().map(this::view).toList());
        return PageResult.of(mapped);
    }
    public List<ExternalOrderVO> calendar(LocalDate start, LocalDate end, Long platform) {
        range(start, end);
        return orders.selectList(filter(start, end, platform, null, null).ne(ExternalOrder::getStatus, ExternalOrderStatus.CANCELLED.name()))
                .stream().map(this::view).toList();
    }
    private static class Accumulator {
        final String name; final Set<Long> ids = new HashSet<>(); long nights;
        BigDecimal gross = BigDecimal.ZERO, platform = BigDecimal.ZERO, anchor = BigDecimal.ZERO;
        Accumulator(String name) { this.name = name; }
        void add(Long id, BigDecimal gross, BigDecimal platform, BigDecimal anchor) {
            ids.add(id); nights++; this.gross = this.gross.add(gross); this.platform = this.platform.add(platform); this.anchor = this.anchor.add(anchor);
        }
        ExternalStatisticsVO.Bucket result() { return new ExternalStatisticsVO.Bucket(name, ids.size(), nights, gross, platform, anchor, gross.subtract(platform).subtract(anchor)); }
    }
    private static List<ExternalStatisticsVO.Bucket> results(Map<String, Accumulator> values) {
        return values.values().stream().map(Accumulator::result).toList();
    }
    /** Step 1: 查询有效订单；Step 2: 逐晚分摊；Step 3: 按维度去重汇总。 */
    public ExternalStatisticsVO statistics(LocalDate start, LocalDate end, Long platform, Long anchor, Long room) {
        range(start, end);
        List<ExternalOrder> data = orders.selectList(filter(start, end, platform, anchor, room)
                .in(ExternalOrder::getStatus, ExternalOrderStatus.CHECKED_IN.name(), ExternalOrderStatus.CHECKED_OUT.name()));
        Accumulator total = new Accumulator("合计");
        Map<String, Accumulator> daily = new TreeMap<>(), byPlatform = new LinkedHashMap<>(), byAnchor = new LinkedHashMap<>(), byRoom = new LinkedHashMap<>();
        for (ExternalOrder order : data) {
            ExternalOrderVO info = view(order);
            int nights = Math.toIntExact(info.nights());
            List<BigDecimal> gross = NightlyAllocation.allocate(order.getTotalAmount(), nights);
            List<BigDecimal> fees = NightlyAllocation.allocate(order.getPlatformFee(), nights);
            List<BigDecimal> commissions = NightlyAllocation.allocate(order.getAnchorFee(), nights);
            for (int i = 0; i < nights; i++) {
                LocalDate day = order.getCheckInDate().plusDays(i);
                if (day.isBefore(start) || day.isAfter(end)) continue;
                List<Accumulator> groups = List.of(total, daily.computeIfAbsent(day.toString(), Accumulator::new),
                        byPlatform.computeIfAbsent(String.valueOf(order.getPlatformId()), k -> new Accumulator(info.platformName())),
                        byAnchor.computeIfAbsent(String.valueOf(order.getAnchorId()), k -> new Accumulator(info.anchorName())),
                        byRoom.computeIfAbsent(String.valueOf(order.getRoomId()), k -> new Accumulator(info.roomName())));
                for (Accumulator group : groups) group.add(order.getId(), gross.get(i), fees.get(i), commissions.get(i));
            }
        }
        return new ExternalStatisticsVO(total.result(), results(daily), results(byPlatform), results(byAnchor), results(byRoom));
    }
}
