package com.homestay.module.file.importer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homestay.module.file.service.FileStorageService;
import com.homestay.module.room.entity.RoomType;
import com.homestay.module.room.mapper.RoomTypeMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 手动执行的房型初始图片导入；默认只预览，不在普通启动时执行。 */
@Component
public class RoomImageImporter implements ApplicationRunner {
    private static final Map<String, String> ROOM_NAMES = new LinkedHashMap<>();
    static {
        ROOM_NAMES.put("101 轻奢", "枕澜·智能轻奢大床房");
        ROOM_NAMES.put("102 浅溪", "浅溪·智能双床房");
        ROOM_NAMES.put("103 云岸", "云岸·智能双床房");
        ROOM_NAMES.put("201 榻榻米", "冷江·榻榻米大床房");
        ROOM_NAMES.put("202 清汀", "清汀·智能双床房");
        ROOM_NAMES.put("203 木荷", "木荷·智能浴缸大床房");
    }
    private final RoomTypeMapper mapper;
    private final FileStorageService storage;
    private final ObjectMapper json;
    private final ConfigurableApplicationContext context;

    public RoomImageImporter(RoomTypeMapper mapper, FileStorageService storage, ObjectMapper json,
                             ConfigurableApplicationContext context) {
        this.mapper = mapper;
        this.storage = storage;
        this.json = json;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (!arguments.containsOption("room-image-import")) { return; }
        Path root = Path.of(arguments.getOptionValues("room-image-import").getFirst());
        boolean execute = arguments.containsOption("room-image-import-execute");
        Map<RoomType, List<Path>> batches = new LinkedHashMap<>();
        // 先核对全部房型和素材，再执行任何上传或数据库写入。
        for (var entry : ROOM_NAMES.entrySet()) {
            List<RoomType> rooms = mapper.selectList(new LambdaQueryWrapper<RoomType>().eq(RoomType::getName, entry.getValue()));
            if (rooms.size() != 1) { throw new IllegalStateException("房型不存在或不唯一: " + entry.getValue()); }
            List<Path> files;
            try (var paths = Files.list(root.resolve(entry.getKey()))) {
                files = paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".jpg"))
                        .sorted().toList();
            }
            if (files.isEmpty()) { throw new IllegalStateException("房间目录没有图片: " + entry.getKey()); }
            batches.put(rooms.getFirst(), files);
            System.out.printf("%s -> %s (ID %d): %d 张，原图片: %s%n", entry.getKey(), entry.getValue(),
                    rooms.getFirst().getId(), files.size(), rooms.getFirst().getImages());
        }
        if (execute) {
            // 每个房型全部上传成功后才更新其图片，失败时保留该房型原有数据。
            for (var batch : batches.entrySet()) {
                var refs = new java.util.ArrayList<String>();
                for (Path file : batch.getValue()) {
                    try (var input = Files.newInputStream(file)) {
                        refs.add(storage.upload(input, file.getFileName().toString(), "image/jpeg"));
                    }
                }
                RoomType update = new RoomType();
                update.setId(batch.getKey().getId());
                update.setImages(json.writeValueAsString(refs));
                mapper.updateById(update);
                System.out.printf("已导入 %s: %d 张%n", batch.getKey().getName(), refs.size());
            }
        } else {
            System.out.println("预览完成；添加 --room-image-import-execute 才会上传并替换图片。");
        }
        context.close();
    }
}
