package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.BlockRepository;
import com.hdfs.namenode.repository.FileRepository;
import com.hdfs.namenode.repository.UserRepository;
import com.hdfs.namenode.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*") // 🟢 هذا السطر هو الذي سيسمح لـ React بسحب المعلومات دون مشاكل
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    // 🟢 تمت الإضافة: حقن مستودع البلوكات للتعامل مع المفتاح الأجنبي
    @Autowired
    private BlockRepository blockRepository;

    // 1. إرجاع حالة النظام (Status)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getClusterStatus() {
        Map<String, Object> status = new HashMap<>();

        long activeWorkersCount = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .count();

        status.put("activeWorkers", activeWorkersCount);
        status.put("totalFiles", fileRepository.count());
        status.put("totalUsers", userRepository.count());
        status.put("health", activeWorkersCount == 0 ? "KRİTİK (Aktif Worker Yok)" : "SAĞLIKLI");

        return ResponseEntity.ok(status);
    }

    // 2. قائمة الخوادم التفصيلية (list-workers)
    @GetMapping("/workers")
    public ResponseEntity<List<Map<String, Object>>> listWorkers() {
        List<Map<String, Object>> workersDetails = workerRepository.findAll().stream().map(w -> {
            Map<String, Object> map = new HashMap<>();
            map.put("url", w.getUrl());
            map.put("active", w.isActive());

            long secondsAgo = java.time.Duration.between(w.getLastHeartbeat(), java.time.LocalDateTime.now()).getSeconds();
            map.put("secondsAgo", secondsAgo);

            map.put("capacity", w.getCapacity());
            map.put("used", w.getUsed());
            map.put("storageInfo", w.getStorageInfo());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(workersDetails);
    }


    // 3. مسح الخادم وإرسال أمر الإغلاق (Remote Shutdown)
    @org.springframework.transaction.annotation.Transactional
    @DeleteMapping("/workers/delete")
    public ResponseEntity<String> deleteWorker(@RequestParam String url) {
        WorkerNode node = workerRepository.findByUrl(url);
        if (node != null) {

            // 1. مسح البلوكات المرتبطة بهذا الخادم لتجنب خطأ Foreign Key
            java.util.List<com.hdfs.namenode.model.BlockMetadata> workerBlocks = blockRepository.findAll().stream()
                    .filter(b -> b.getWorker().getId().equals(node.getId()))
                    .collect(Collectors.toList());

            if (!workerBlocks.isEmpty()) {
                blockRepository.deleteAll(workerBlocks);
                System.out.println("⚠️ Admin uyarısı: Silinen worker'a ait " + workerBlocks.size() + " blok kaydı temizlendi.");
            }

            // 2. 🟢 إرسال أمر "الإغلاق الذاتي" (Shutdown) للووركر عبر الشبكة
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                String shutdownUrl = node.getUrl() + "/api/worker/shutdown";
                restTemplate.postForEntity(shutdownUrl, null, String.class);
                System.out.println("🛑 Worker'a kapanma emri başarıyla gönderildi: " + node.getUrl());
            } catch (Exception e) {
                System.out.println("⚠️ Uyarı: Worker'a kapanma emri iletilemedi (Zaten kapalı veya ulaşılamıyor olabilir).");
            }

            // 3. مسح الخادم من قاعدة بيانات الماستر
            workerRepository.delete(node);

            return ResponseEntity.ok("✅ Çalışan düğüm başarıyla veritabanından silindi ve KAPANMA emri gönderildi: " + url);
        } else {
            return ResponseEntity.status(404).body("❌ Çalışan düğüm bulunamadı.");
        }
    }
}