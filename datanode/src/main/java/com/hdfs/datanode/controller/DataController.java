package com.hdfs.datanode.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/data")
public class DataController {

    // 🟢 نقرأ البورت لكي نجعل مجلد التخزين خاصاً بكل وركر
    @Value("${server.port}")
    private String serverPort;

    // دالة مساعدة لجلب مسار التخزين بناءً على البورت
    // النتيجة ستكون: ./data/worker_8081/
    private String getStorageDir() {
        return "./data/worker_" + serverPort + "/";
    }

    // 💓 1. نقطة فحص الصحة (Heartbeat / Health Check)
    @GetMapping("/health")
    public ResponseEntity<String> checkHealth() {
        return ResponseEntity.ok("UP");
    }

    // 📥 2. رفع الملفات (Write)
    @PostMapping("/write")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String currentDir = getStorageDir();

            File directory = new File(currentDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            Path filepath = Paths.get(currentDir + file.getOriginalFilename());
            file.transferTo(filepath);

            System.out.println("💾 Dosya kaydedildi: " + filepath.toString());
            return "Success";

        } catch (IOException e) {
            e.printStackTrace();
            return "Failed: " + e.getMessage();
        }
    }

    // 📤 3. تحميل الملفات (Read)
    @GetMapping("/read/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(getStorageDir() + filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                System.out.println("📤 Dosya gönderiliyor: " + filename);

                String encodedFilename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8.toString());

                return ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("Dosya bulunamadı!");
            }
        } catch (MalformedURLException | java.io.UnsupportedEncodingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 🗑️ 4. حذف الملفات (Delete - النسخة المعدلة) 🟢
    // التغيير هنا: بدلاً من حذف اسم الملف بالضبط، نبحث عن كل الأجزاء ونحذفها
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        System.out.println("🗑️ Silme isteği alındı: " + filename);

        try {
            // 1. الوصول للمجلد
            File folder = new File(getStorageDir());

            if (!folder.exists() || !folder.isDirectory()) {
                return ResponseEntity.status(500).body("Depolama klasörü bulunamadı!");
            }

            // 2. الفلترة الذكية: نجد الملفات التي تبدأ بالاسم المطلوب
            // (مثلاً: video.mkv_part_1, video.mkv_part_2...)
            File[] matchingFiles = folder.listFiles((dir, name) ->
                    name.equals(filename) || name.startsWith(filename + "_part_")
            );

            if (matchingFiles == null || matchingFiles.length == 0) {
                // إذا لم نجد شيئاً، نعتبر العملية ناجحة (لأن الهدف تحقق والملف غير موجود)
                return ResponseEntity.ok("Dosya zaten yok (Already deleted).");
            }

            // 3. حذف كل الملفات التي وجدناها
            int deletedCount = 0;
            for (File file : matchingFiles) {
                if (file.delete()) {
                    System.out.println("   ✅ Silindi: " + file.getName());
                    deletedCount++;
                } else {
                    System.out.println("   ❌ Silinemedi: " + file.getName());
                }
            }

            return ResponseEntity.ok(deletedCount + " parça başarıyla silindi.");

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Hata: " + e.getMessage());
        }
    }
}