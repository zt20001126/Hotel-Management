package com.homestay.module.system.service;

import com.homestay.common.result.PageResult;
import com.homestay.module.system.dto.OperatingExpenseRequest;
import com.homestay.module.system.vo.*;
import java.time.LocalDateTime;

/** 经营支出业务接口。 */
public interface OperatingExpenseService {
    void create(OperatingExpenseRequest request);
    PageResult<OperatingExpenseVO> list(int page, int size, String keyword, LocalDateTime startTime, LocalDateTime endTime);
    void update(Long id, OperatingExpenseRequest request);
    void delete(Long id);
    OperatingExpenseStatisticsVO statistics(String keyword, LocalDateTime startTime, LocalDateTime endTime);
}
