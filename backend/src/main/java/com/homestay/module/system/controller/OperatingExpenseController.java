package com.homestay.module.system.controller;

import com.homestay.common.exception.BusinessException;
import com.homestay.common.result.*;
import com.homestay.context.UserContextHolder;
import com.homestay.module.system.dto.OperatingExpenseRequest;
import com.homestay.module.system.service.OperatingExpenseService;
import com.homestay.module.system.vo.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

/** 经营支出接口，仅向管理员开放。 */
@RestController
@RequestMapping("/operating-expenses")
public class OperatingExpenseController {
    private static final String OWNER = "owner";
    private static final String EMPLOYEE = "employee";
    private final OperatingExpenseService service;
    public OperatingExpenseController(OperatingExpenseService service) { this.service = service; }
    private void requireOwner() {
        if (!OWNER.equals(UserContextHolder.getRole()) || !EMPLOYEE.equals(UserContextHolder.getUserType())) throw new BusinessException(403, "仅管理员可管理经营支出");
    }
    @PostMapping
    public Result<Void> create(@Valid @RequestBody OperatingExpenseRequest request) { requireOwner(); service.create(request); return Result.success(); }
    @GetMapping
    public Result<PageResult<OperatingExpenseVO>> list(@RequestParam(defaultValue="1") int page, @RequestParam(defaultValue="10") int size,
            @RequestParam(required=false) String keyword,
            @RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        requireOwner(); return Result.success(service.list(page, size, keyword, startTime, endTime));
    }
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody OperatingExpenseRequest request) { requireOwner(); service.update(id, request); return Result.success(); }
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) { requireOwner(); service.delete(id); return Result.success(); }
    @GetMapping("/statistics")
    public Result<OperatingExpenseStatisticsVO> statistics(@RequestParam(required=false) String keyword,
            @RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required=false) @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        requireOwner(); return Result.success(service.statistics(keyword, startTime, endTime));
    }
}
