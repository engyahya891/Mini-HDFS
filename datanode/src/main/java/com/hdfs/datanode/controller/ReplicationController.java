package com.hdfs.datanode.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Map;

@RestController
@RequestMapping("/api/file") // هذا المسار الذي يرسل عليه الماستر (لا تغيره)
public class ReplicationController {

    @Value("${server.port}")
    private String serverPort;

    // دالة مساعدة لمعرفة مكان التخزين (نفس المنطق الموجود في DataController)
    private String getStorageDir() {
        return "./data/worker_" + serverPort + "/";
    }

    // دالة استقبال أمر النسخ من الماستر
    @PostMapping("/replicate")
    public ResponseEntity<String> replicateBlock(@RequestBody Map<String, String> request) {

        String blockId = request.get("blockId");
        String targetWorkerUrl = request.get("targetUrl");

        System.out.println("📥 [REPLICATION] Master'dan emir alındı: " + blockId + " -> " + targetWorkerUrl);

        // 1. البحث عن الملف في التخزين المحلي
        String filePath = getStorageDir() + blockId;
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("❌ Hata: İstenen blok dosyası bulunamadı: " + filePath);
            return ResponseEntity.badRequest().body("Dosya bulunamadı");
        }

        // 2. تجهيز الإرسال إلى الـ Worker الهدف
        RestTemplate restTemplate = new RestTemplate();

        // ⚠️ تعديل هام جداً: هنا نستخدم الرابط الموجود في DataController الخاص بك
        String targetUploadUrl = targetWorkerUrl + "/api/data/write";

        try {
            // إعداد الهيدر ليكون multipart/form-data
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // تجهيز الجسم (Body)
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            // المفتاح "file" يجب أن يطابق @RequestParam("file") في DataController
            body.add("file", new FileSystemResource(file));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 3. الإرسال الفعلي!
            System.out.println("📤 Dosya yönlendiriliyor: " + targetUploadUrl);
            String response = restTemplate.postForObject(targetUploadUrl, requestEntity, String.class);

            System.out.println("✅ Replikasyon Başarılı! Hedef yanıtı: " + response);
            return ResponseEntity.ok("Replikasyon tamamlandı");

        } catch (Exception e) {
            System.out.println("❌ Replikasyon başarısız: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Hata: " + e.getMessage());
        }
    }
}