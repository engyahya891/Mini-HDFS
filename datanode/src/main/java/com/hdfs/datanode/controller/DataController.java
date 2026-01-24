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
        System.out.println("\n📤 ------------------------------------------------ 📤");
        System.out.println("📡 DOSYA İSTEĞİ (Download Request)");
        System.out.println("📄 İstenen Dosya: " + filename);

        try {
            Path filePath = Paths.get(getStorageDir() + filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                System.out.println("✅ DOSYA BULUNDU VE GÖNDERİLİYOR...");

                String encodedFilename = URLEncoder.encode(resource.getFilename(), StandardCharsets.UTF_8.toString());
                System.out.println("📤 ------------------------------------------------ 📤\n");

                return ResponseEntity.ok()
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                        .body(resource);
            } else {
                System.out.println("❌ HATA: Dosya bulunamadı!");
                throw new RuntimeException("Dosya bulunamadı!");
            }
        } catch (MalformedURLException | java.io.UnsupportedEncodingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 🗑️ حذف ملف (Delete)
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        System.out.println("\n🗑️ ------------------------------------------------ 🗑️");
        System.out.println("❌ SİLME İSTEĞİ (Delete Request)");
        System.out.println("📄 Dosya: " + filename);

        try {
            Path filePath = Paths.get(getStorageDir() + filename);
            boolean deleted = java.nio.file.Files.deleteIfExists(filePath);

            if (deleted) {
                System.out.println("✅ BAŞARILI: Dosya diskten silindi.");
                System.out.println("🗑️ ------------------------------------------------ 🗑️\n");
                return ResponseEntity.ok("File deleted successfully");
            } else {
                System.out.println("⚠️ UYARI: Dosya zaten yok.");
                return ResponseEntity.status(404).body("File not found on disk");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting file: " + e.getMessage());
        }
    }
}