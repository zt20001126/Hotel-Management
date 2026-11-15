package com.homestay.module.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.module.file.service.FileStorageService;
import com.homestay.module.room.entity.RoomType;
import com.homestay.module.room.service.RoomTypeImageService;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/** 验证稳定图片标识不会被临时访问地址覆盖。 */
class RoomTypeImageServiceTest {
    @Test
    void returnsSignedUrlsAndKeepsStableReferencesInOrder() {
        FileStorageService storage = mock(FileStorageService.class);
        when(storage.accessUrl("oss://bucket/rooms/a.jpg")).thenReturn("https://example.com/a?signature=1");
        when(storage.accessUrl("https://example.com/old.jpg")).thenReturn("https://example.com/old.jpg");
        RoomType room = new RoomType();
        room.setImages("[\"oss://bucket/rooms/a.jpg\",\"https://example.com/old.jpg\"]");
        var result = new RoomTypeImageService(new ObjectMapper(), storage).toResponse(room);
        assertEquals(List.of("oss://bucket/rooms/a.jpg", "https://example.com/old.jpg"), result.getImageRefs());
        assertEquals("[\"https://example.com/a?signature=1\",\"https://example.com/old.jpg\"]", result.getImages());
        assertEquals("[\"oss://bucket/rooms/a.jpg\",\"https://example.com/old.jpg\"]", room.getImages());
    }
}
