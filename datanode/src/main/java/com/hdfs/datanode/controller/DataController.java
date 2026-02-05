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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

@RestController
@RequestMapping("/api/data")
public class DataController {

    @Value("${server.port}")
    private String serverPort;

    private String getStorageDir() {
        return "./data/worker_" + serverPort + "/";
    }

    @GetMapping("/health")
    public ResponseEntity<String> checkHealth() {
        return ResponseEntity.ok("UP");
    }

    // 📥 Yazma (Write)
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

            return "Başarılı";
        } catch (IOException e) {
            return "Başarısız";
        }
    }

    // 📤 Okuma (Read)
    @GetMapping("/read/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        try {
            String decodedFileName =
                    URLDecoder.decode(filename, StandardCharsets.UTF_8.toString())
                            .replace("+", " ");

            Path filePath = Paths.get(getStorageDir())
                    .resolve(decodedFileName)
                    .normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {

                String safeFilename = URLEncoder
                        .encode(resource.getFilename(), StandardCharsets.UTF_8.toString())
                        .replace("+", "%20");

                return ResponseEntity.ok()
                        .header(
                                HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + safeFilename + "\""
                        )
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 🗑️ Silme (Delete)
    @DeleteMapping("/delete/{base64Filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String base64Filename) {
        try {
            // Base64 çöz
            byte[] decodedBytes = Base64.getUrlDecoder().decode(base64Filename);
            String tempName = new String(decodedBytes, StandardCharsets.UTF_8);

            // URL decode
            String originalName =
                    URLDecoder.decode(tempName, StandardCharsets.UTF_8.toString());

            System.out.println("🗑️ Silme isteği (gerçek dosya adı): " + originalName);

            File directory = new File(getStorageDir());
            File[] matches =
                    directory.listFiles((dir, name) -> name.startsWith(originalName));

            if (matches != null && matches.length > 0) {
                int count = 0;
                for (File f : matches) {
                    if (f.delete()) {
                        count++;
                    }
                }

                System.out.println("✅ Fiziksel silme tamamlandı: " + count + " dosya.");
                return ResponseEntity.ok(count + " dosya silindi.");
            } else {
                System.out.println("ℹ️ Dosya bulunamadı veya zaten silinmiş.");
                return ResponseEntity.ok("Dosya zaten silinmiş.");
            }

        } catch (Exception e) {
            System.err.println("❌ Hata: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Hata: " + e.getMessage());
        }
    }
}
