package com.homestay.module.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homestay.common.exception.BusinessException;
import com.homestay.common.result.PageResult;
import com.homestay.module.system.dto.OperatingExpenseRequest;
import com.homestay.module.system.entity.OperatingExpense;
import com.homestay.module.system.mapper.OperatingExpenseMapper;
import com.homestay.module.system.service.OperatingExpenseService;
import com.homestay.module.system.vo.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/** 经营支出业务实现。 */
@Service
public class OperatingExpenseServiceImpl implements OperatingExpenseService {
    private static final int MAX_PAGE_SIZE = 100;
    private final OperatingExpenseMapper mapper;
    public OperatingExpenseServiceImpl(OperatingExpenseMapper mapper) { this.mapper = mapper; }

    /** Step 1: 转换输入；Step 2: 保存支出。 */
    public void create(OperatingExpenseRequest request) {
        OperatingExpense expense = new OperatingExpense();
        expense.setContent(request.getContent().trim());
        expense.setAmount(request.getAmount());
        mapper.insert(expense);
    }
    /** Step 1: 检查筛选范围；Step 2: 查询分页。 */
    public PageResult<OperatingExpenseVO> list(int page, int size, String keyword, LocalDateTime startTime, LocalDateTime endTime) {
        validateRange(startTime, endTime);
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) throw new BusinessException(400, "分页参数无效");
        return PageResult.of(mapper.selectExpensePage(new Page<>(page, size), keyword, startTime, endTime));
    }
    /** Step 1: 修改指定记录；Step 2: 检查记录是否存在。 */
    public void update(Long id, OperatingExpenseRequest request) {
        OperatingExpense expense = new OperatingExpense();
        expense.setId(id);
        expense.setContent(request.getContent().trim());
        expense.setAmount(request.getAmount());
        // 受影响行数为零时区分不存在与内容未改变。
        if (mapper.updateById(expense) == 0 && mapper.selectById(id) == null) throw new BusinessException(404, "支出记录不存在");
    }
    /** Step 1: 删除指定记录；Step 2: 检查记录是否存在。 */
    public void delete(Long id) {
        if (mapper.deleteById(id) == 0) throw new BusinessException(404, "支出记录不存在");
    }
    /** Step 1: 检查筛选范围；Step 2: 汇总全部匹配记录。 */
    public OperatingExpenseStatisticsVO statistics(String keyword, LocalDateTime startTime, LocalDateTime endTime) {
        validateRange(startTime, endTime);
        return mapper.statistics(keyword, startTime, endTime);
    }
    private void validateRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && !startTime.isBefore(endTime)) throw new BusinessException(400, "开始时间必须早于结束时间");
    }
}
