package com.hdfs.datanode.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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

    @Value("${server.port}")
    private String serverPort;

    private String getStorageDir() {
        return "./data/worker_" + serverPort + "/";
    }

    // 💓 فحص الصحة
    @GetMapping("/health")
    public ResponseEntity<String> checkHealth() {
        // لن نطبع شيئاً هنا لكي لا نزعجك بكثرة الرسائل في الكونسول
        // لأن الماستر ينادي هذه الدالة كل 10 ثواني
        return ResponseEntity.ok("UP");
    }

    // 📥 استقبال ملف (Upload)
    @PostMapping("/write")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        System.out.println("\n🔽 ------------------------------------------------ 🔽");
        System.out.println("📥 DOSYA GELİYOR! (Receiving File)");
        System.out.println("📄 Dosya Adı: " + file.getOriginalFilename());
        System.out.println("📦 Boyut: " + file.getSize() + " bytes");

        try {
            String currentDir = getStorageDir();
            File directory = new File(currentDir);
            if (!directory.exists()) directory.mkdirs();

            Path filepath = Paths.get(currentDir + file.getOriginalFilename());
            file.transferTo(filepath);

            System.out.println("✅ KAYDEDİLDİ: " + filepath.toString());
            System.out.println("🔼 ------------------------------------------------ 🔼\n");
            return "Success";

        } catch (IOException e) {
            System.out.println("❌ HATA (Upload Failed): " + e.getMessage());
            e.printStackTrace();
            return "Failed: " + e.getMessage();
        }
    }

    // 📤 إرسال ملف (Download)
    @GetMapping("/read/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            // فك تشفير الاسم للتعامل مع المسافات والرموز الخاصة
            String decodedFileName = java.net.URLDecoder.decode(filename, StandardCharsets.UTF_8.toString());
            Path filePath = Paths.get(getStorageDir()).resolve(decodedFileName).normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                // سجل المسار الكامل في الـ Console لتعرف أين يبحث الـ Worker بالضبط
                System.err.println("❌ Dosya bulunamadı: " + filePath.toAbsolutePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 🗑️ حذف ملف (Delete)
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        try {
            // 1. فك تشفير الاسم للتعامل مع المسافات واللغة العربية %20
            String decodedName = java.net.URLDecoder.decode(filename, "UTF-8");
            File directory = new File(getStorageDir());

            // 2. البحث عن كل الملفات التي تبدأ بهذا الاسم (لحذف part_1, part_2...)
            File[] matches = directory.listFiles((dir, name) -> name.startsWith(decodedName));

            if (matches != null && matches.length > 0) {
                int count = 0;
                for (File f : matches) {
                    if (f.delete()) count++;
                }
                return ResponseEntity.ok("✅ " + count + " parça fiziksel olarak silindi.");
            } else {
                return ResponseEntity.status(404).body("⚠️ Dosya bulunamadı.");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Hata: " + e.getMessage());
        }
    }
}