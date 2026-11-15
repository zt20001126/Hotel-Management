package com.homestay.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.homestay.module.system.entity.OperatingExpense;
import com.homestay.module.system.vo.OperatingExpenseVO;
import com.homestay.module.system.vo.OperatingExpenseStatisticsVO;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;

/** 经营支出数据访问。 */
public interface OperatingExpenseMapper extends BaseMapper<OperatingExpense> {
    IPage<OperatingExpenseVO> selectExpensePage(IPage<OperatingExpenseVO> page, @Param("keyword") String keyword,
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    OperatingExpenseStatisticsVO statistics(@Param("keyword") String keyword,
            @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
