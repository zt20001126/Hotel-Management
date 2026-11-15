package com.homestay.module.external.controller;

import com.homestay.common.exception.BusinessException;
import com.homestay.common.result.PageResult;
import com.homestay.common.result.Result;
import com.homestay.context.UserContextHolder;
import com.homestay.module.external.dto.*;
import com.homestay.module.external.service.ExternalService;
import com.homestay.module.external.vo.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.LocalDate;
import java.util.List;

/** 第三方经营接口，仅经营者使用。 */
@RestController @RequestMapping("/external")
public class ExternalController {
    private static final String OWNER = "owner";
    private static final String EMPLOYEE = "employee";
    private final ExternalService service;
    public ExternalController(ExternalService service) { this.service = service; }
    /** 将第三方接口的参数解析错误限定为客户端错误，避免影响其他模块。 */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> invalidRequest(Exception exception) {
        return Result.error(400, "请求参数格式错误或缺少必填参数");
    }
    private void requireOwner() {
        if (!OWNER.equals(UserContextHolder.getRole()) || !EMPLOYEE.equals(UserContextHolder.getUserType()))
            throw new BusinessException(403, "仅经营者可访问第三方平台经营");
    }
    @GetMapping("/platforms") public Result<List<ExternalCatalogVO>> platforms(@RequestParam(defaultValue="false") boolean includeDisabled) {
        requireOwner(); return Result.success(service.platforms(includeDisabled));
    }
    @PostMapping("/platforms") public Result<Long> addPlatform(@Valid @RequestBody ExternalCatalogRequest request) {
        requireOwner(); return Result.success(service.savePlatform(null, request));
    }
    @PutMapping("/platforms/{id}") public Result<Long> updatePlatform(@PathVariable Long id, @Valid @RequestBody ExternalCatalogRequest request) {
        requireOwner(); return Result.success(service.savePlatform(id, request));
    }
    @GetMapping("/anchors") public Result<List<ExternalCatalogVO>> anchors(@RequestParam(defaultValue="false") boolean includeDisabled) {
        requireOwner(); return Result.success(service.anchors(includeDisabled));
    }
    @PostMapping("/anchors") public Result<Long> addAnchor(@Valid @RequestBody ExternalCatalogRequest request) {
        requireOwner(); return Result.success(service.saveAnchor(null, request));
    }
    @PutMapping("/anchors/{id}") public Result<Long> updateAnchor(@PathVariable Long id, @Valid @RequestBody ExternalCatalogRequest request) {
        requireOwner(); return Result.success(service.saveAnchor(id, request));
    }
    @GetMapping("/orders") public Result<PageResult<ExternalOrderVO>> orders(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="10") int size, @RequestParam(required=false) LocalDate startDate,
            @RequestParam(required=false) LocalDate endDate, @RequestParam(required=false) Long platformId,
            @RequestParam(required=false) Long anchorId, @RequestParam(required=false) Long roomId,
            @RequestParam(required=false) String status, @RequestParam(required=false) String keyword) {
        requireOwner(); return Result.success(service.orders(page, size, startDate, endDate, platformId, anchorId, roomId, status, keyword));
    }
    @GetMapping("/orders/{id}") public Result<ExternalOrderVO> order(@PathVariable Long id) {
        requireOwner(); return Result.success(service.order(id));
    }
    @PostMapping("/orders") public Result<Long> create(@Valid @RequestBody ExternalOrderRequest request) {
        requireOwner(); return Result.success(service.createOrder(request));
    }
    @PutMapping("/orders/{id}") public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ExternalOrderRequest request) {
        requireOwner(); service.updateOrder(id, request); return Result.success();
    }
    @PostMapping("/orders/{id}/status") public Result<Void> status(@PathVariable Long id, @Valid @RequestBody ExternalStatusRequest request) {
        requireOwner(); service.changeStatus(id, request.getStatus()); return Result.success();
    }
    @GetMapping("/calendar") public Result<List<ExternalOrderVO>> calendar(@RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate, @RequestParam(required=false) Long platformId) {
        requireOwner(); return Result.success(service.calendar(startDate, endDate, platformId));
    }
    @GetMapping("/statistics") public Result<ExternalStatisticsVO> statistics(@RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate, @RequestParam(required=false) Long platformId,
            @RequestParam(required=false) Long anchorId, @RequestParam(required=false) Long roomId) {
        requireOwner(); return Result.success(service.statistics(startDate, endDate, platformId, anchorId, roomId));
    }
}
