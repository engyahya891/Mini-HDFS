package com.hdfs.datanode;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/data")
public class DataController {

    // تحديد المجلد الذي سيحفظ فيه الووركر الملفات
    // يمكنك تغييره لأي مسار تريده في جهازك
    private final String STORAGE_DIR = "C:/mini-hdfs-storage/";

    @PostMapping("/write")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 1. التأكد من وجود مجلد التخزين، وإنشاؤه إذا لم يكن موجوداً
            File directory = new File(STORAGE_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 2. تحديد المسار الكامل للملف
            Path filepath = Paths.get(STORAGE_DIR + file.getOriginalFilename());

            // 3. حفظ الملف فعلياً على القرص (هذا هو السطر السحري)
            file.transferTo(filepath);

            System.out.println("💾 Dosya kaydedildi: " + filepath.toString());
            return "Success";

        } catch (IOException e) {
            e.printStackTrace();
            return "Failed: " + e.getMessage();
        }
    }
}