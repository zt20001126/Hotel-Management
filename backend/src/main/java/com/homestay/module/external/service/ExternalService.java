package com.homestay.module.external.service;

import com.homestay.common.result.PageResult;
import com.homestay.module.external.dto.*;
import com.homestay.module.external.vo.*;
import java.time.LocalDate;
import java.util.List;

/** 第三方平台经营功能边界。 */
public interface ExternalService {
    List<ExternalCatalogVO> platforms(boolean includeDisabled);
    List<ExternalCatalogVO> anchors(boolean includeDisabled);
    Long savePlatform(Long id, ExternalCatalogRequest request);
    Long saveAnchor(Long id, ExternalCatalogRequest request);
    Long createOrder(ExternalOrderRequest request);
    void updateOrder(Long id, ExternalOrderRequest request);
    void changeStatus(Long id, String status);
    ExternalOrderVO order(Long id);
    PageResult<ExternalOrderVO> orders(int page, int size, LocalDate startDate, LocalDate endDate,
                                       Long platformId, Long anchorId, Long roomId, String status, String keyword);
    PageResult<ExternalOrderVO> unifiedOrders(int page, int size, LocalDate startDate, LocalDate endDate,
                                              Long platformId, Long anchorId, Long roomId, String status, String keyword);
    List<ExternalOrderVO> calendar(LocalDate startDate, LocalDate endDate, Long platformId);
    ExternalStatisticsVO statistics(LocalDate startDate, LocalDate endDate, Long platformId, Long anchorId, Long roomId);
}
