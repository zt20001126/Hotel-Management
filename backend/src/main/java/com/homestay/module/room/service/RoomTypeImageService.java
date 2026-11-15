package com.homestay.module.room.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.common.exception.BusinessException;
import com.homestay.module.file.service.FileStorageService;
import com.homestay.module.room.entity.RoomType;
import com.homestay.module.room.vo.RoomTypeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/** 房型图片引用与临时访问地址的转换。 */
@Service
public class RoomTypeImageService {
    private final ObjectMapper mapper;
    private final FileStorageService storage;
    public RoomTypeImageService(ObjectMapper mapper, FileStorageService storage) {
        this.mapper = mapper;
        this.storage = storage;
    }

    /** 保留稳定引用供后台编辑，同时向客户端提供临时访问地址。 */
    public RoomTypeVO toResponse(RoomType roomType) {
        if (roomType == null) { return null; }
        RoomTypeVO result = new RoomTypeVO();
        BeanUtils.copyProperties(roomType, result);
        try {
            List<String> refs = roomType.getImages() == null || roomType.getImages().isBlank()
                    ? List.of() : mapper.readValue(roomType.getImages(), new TypeReference<>() {});
            result.setImageRefs(refs);
            result.setImages(mapper.writeValueAsString(refs.stream().map(storage::accessUrl).toList()));
            return result;
        } catch (JsonProcessingException e) {
            throw new BusinessException("房型图片数据格式错误");
        }
    }
}
