package com.hdfs.namenode;

import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import com.hdfs.namenode.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hdfs.common.protocol.BlockAllocation; // 🟢 استيراد الكلاس المشترك

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private FileRepository fileRepository;

    // 🟢 1. دالة حجز البلوك (الدمج: تقطيع + تكرار)
    // العميل يرسل رقم البلوك، والماستر يرد بـ "قائمة" سيرفرات لرفع النسخ إليها
    @PostMapping("/allocate-block")
    public ResponseEntity<BlockAllocation> allocateBlock(@RequestBody BlockAllocation requestInfo) {

        System.out.println("📦 Blok Yerleştirme İsteği: Blok #" + requestInfo.getBlockIndex());

        // أ) جلب العمال النشطين
        List<WorkerNode> activeWorkers = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .collect(Collectors.toList());

        if (activeWorkers.isEmpty()) {
            System.out.println("❌ HATA: Aktif worker yok!");
            return ResponseEntity.status(500).build();
        }

        // ب) خلط القائمة (Load Balancing)
        Collections.shuffle(activeWorkers);

        // ج) التكرار (Replication Factor = 2)
        // نختار أول خادمين متاحين
        int replicationFactor = Math.min(activeWorkers.size(), 2);

        List<String> selectedWorkerUrls = activeWorkers.stream()
                .limit(replicationFactor)
                .map(WorkerNode::getUrl)
                .collect(Collectors.toList());

        System.out.println("   ✅ Hedef Sunucular: " + selectedWorkerUrls);

        // د) إرجاع الرد (يحتوي على رقم البلوك + قائمة الروابط)
        BlockAllocation response = new BlockAllocation(requestInfo.getBlockIndex(), selectedWorkerUrls);

        return ResponseEntity.ok(response);
    }

    // 🟢 2. دالة التوزيع البسيط (احتياطية للرفع بدون تقطيع)
    @GetMapping("/assign-workers")
    public ResponseEntity<List<String>> assignWorkers() {
        List<WorkerNode> activeWorkers = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .collect(Collectors.toList());

        if (activeWorkers.isEmpty()) return ResponseEntity.status(500).build();

        Collections.shuffle(activeWorkers);
        int replicationFactor = Math.min(activeWorkers.size(), 2);

        List<String> selectedUrls = activeWorkers.subList(0, replicationFactor).stream()
                .map(WorkerNode::getUrl)
                .collect(Collectors.toList());

        return ResponseEntity.ok(selectedUrls);
    }

    // 🟢 3. تحديد مكان الملف (للتنزيل)
    @GetMapping("/locate/{filename}")
    public String locateFile(@PathVariable String filename) {
        // للتبسيط، نرجع رابط أي وركر نشط، لأن الملف موجود عند الجميع
        return workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .findFirst()
                .map(WorkerNode::getUrl)
                .orElse("DOSYA_BULUNAMADI");
    }

    // 🟢 4. الحذف الجماعي (Global Delete)
    @DeleteMapping("/delete/{filename}")
    public void deleteFile(@PathVariable String filename) {
        System.out.println("🗑️ Global Silme İsteği: " + filename);

        List<WorkerNode> workers = workerRepository.findAll();
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

        for (WorkerNode worker : workers) {
            if (worker.isActive()) {
                try {
                    restTemplate.delete(worker.getUrl() + "/api/data/delete/" + filename);
                    System.out.println("✅ Silindi: " + worker.getUrl());
                } catch (Exception e) {
                    System.out.println("⚠️ Silinemedi: " + worker.getUrl());
                }
            }
        }
    }
}
/*
* 💡 ماذا صححت لك؟
أضفت دالة allocateBlock (مع @PostMapping): هذه هي الدالة الأساسية الجديدة التي سيستخدمها العميل المطور لرفع البلوكات.

أبقيت على assignWorkers: تحسباً لو أردت اختبار رفع بسيط في المستقبل.

تأكدت من استيراد BlockAllocation.
*
* */

