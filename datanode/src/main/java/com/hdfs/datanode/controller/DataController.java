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
    // هذه هي الإضافة الجديدة التي يطلبها الماستر للتأكد أن الوركر حي
    @GetMapping("/health")
    public ResponseEntity<String> checkHealth() {
        return ResponseEntity.ok("UP");
    }

    // 📥 2. رفع الملفات (Write)
    @PostMapping("/write")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String currentDir = getStorageDir(); // نستخدم المسار الديناميكي

            // التأكد من وجود المجلد
            File directory = new File(currentDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // حفظ الملف
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

                // تشفير الاسم ليدعم العربية عند التنزيل
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

    // 🗑️ 4. حذف الملفات (Delete)
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(getStorageDir() + filename);
            boolean deleted = java.nio.file.Files.deleteIfExists(filePath);

            if (deleted) {
                System.out.println("🗑️ Dosya silindi: " + filename);
                return ResponseEntity.ok("File deleted successfully");
            } else {
                return ResponseEntity.status(404).body("File not found on disk");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting file: " + e.getMessage());
        }
    }
}