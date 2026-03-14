package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.FileRepository;
import com.hdfs.namenode.repository.UserRepository;
import com.hdfs.namenode.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    // 🟢 استخدام المستودعات الصحيحة الموجودة في مشروعك
    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    // 1. إرجاع حالة النظام (Status)
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getClusterStatus() {
        Map<String, Object> status = new HashMap<>();

        // حساب عدد الووركرز النشطين فقط
        long activeWorkersCount = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .count();

        status.put("Aktif Workerlar : ", activeWorkersCount);
        status.put("Toplam Dosya Sayısı : ", fileRepository.count());
        status.put("Toplam Kulanıcı sayısı : ", userRepository.count());
        status.put("Sistem Sağlığı : ", activeWorkersCount == 0 ? "Kritik (Aktif Worker Yok)" : "SAĞLIKLI (Healthy)");

        return ResponseEntity.ok(status);
    }

    // 2. قائمة الخوادم (list-workers)
    @GetMapping("/workers")
    public ResponseEntity<Set<String>> listWorkers() {
        // إرجاع روابط (URLs) الووركرز النشطين فقط ليعرضها العميل
        Set<String> activeWorkerUrls = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .map(WorkerNode::getUrl)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(activeWorkerUrls);
    }

    // 3. حذف خادم (delete-worker)
    @DeleteMapping("/workers/delete")
    public ResponseEntity<String> deleteWorker(@RequestParam String url) {
        WorkerNode node = workerRepository.findByUrl(url);
        if (node != null) {
            workerRepository.delete(node);
            return ResponseEntity.ok("✅ Worker başarıyla silindi: " + url);
        } else {
            return ResponseEntity.status(404).body("❌ Çalışan düğüm bulunamadı (Worker Not Found).");
        }
    }
}