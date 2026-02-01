package com.hdfs.namenode;

import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import com.hdfs.common.protocol.BlockAllocation; // ✅ تأكد أن هذا الكلاس موجود في common

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private WorkerRepository workerRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // 🟢 1. دالة حجز البلوك (لعملية الرفع مع التقطيع)
    @PostMapping("/allocate-block")
    public ResponseEntity<BlockAllocation> allocateBlock(@RequestBody BlockAllocation requestInfo) {

        System.out.println("📦 Master: طلب حجز مكان للبلوك رقم: " + requestInfo.getBlockIndex());

        // أ) جلب العمال النشطين فقط
        List<WorkerNode> activeWorkers = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .collect(Collectors.toList());

        if (activeWorkers.isEmpty()) {
            System.out.println("❌ HATA: لا يوجد عمال نشطين (Active Workers)!");
            return ResponseEntity.status(500).build();
        }

        // ب) خلط القائمة لتوزيع الحمل (Load Balancing)
        Collections.shuffle(activeWorkers);

        // ج) تحديد معامل التكرار (Replication Factor = 2)
        int replicationFactor = Math.min(activeWorkers.size(), 2);

        // د) اختيار العمال
        List<String> selectedWorkerUrls = activeWorkers.stream()
                .limit(replicationFactor)
                .map(WorkerNode::getUrl)
                .collect(Collectors.toList());

        System.out.println("   ✅ تم توجيه البلوك إلى: " + selectedWorkerUrls);

        // هـ) إرجاع الرد للعميل
        BlockAllocation response = new BlockAllocation();
        response.setBlockIndex(requestInfo.getBlockIndex());
        response.setWorkerUrls(selectedWorkerUrls);

        return ResponseEntity.ok(response);
    }

    // 🟢 2. دالة التوزيع البسيط (احتياطية)
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
        // نرجع رابط أول وركر نشط (لأننا نفترض أن الملف منسوخ عند الجميع حالياً)
        return workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .findFirst()
                .map(WorkerNode::getUrl)
                .orElse("DOSYA_BULUNAMADI");
    }

    // 🟢 4. الحذف الجماعي (Global Delete) - تم التصحيح ✅
    // التعديل: جعلناها ترجع ResponseEntity بدلاً من void لتجنب الأخطاء في العميل
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        System.out.println("🗑️ Master: تعميم أمر حذف الملف -> " + filename);

        List<WorkerNode> workers = workerRepository.findAll();
        int successCount = 0;

        for (WorkerNode worker : workers) {
            // نرسل الأمر فقط للعمال النشطين
            if (worker.isActive()) {
                try {
                    String workerDeleteUrl = worker.getUrl() + "/api/data/delete/" + filename;
                    restTemplate.delete(workerDeleteUrl);

                    System.out.println("   ✅ تم الحذف من الوركر: " + worker.getUrl());
                    successCount++;
                } catch (Exception e) {
                    System.out.println("   ⚠️ فشل الحذف من الوركر: " + worker.getUrl());
                }
            }
        }

        if (successCount > 0) {
            return ResponseEntity.ok("تم حذف الملف من " + successCount + " سيرفر.");
        } else {
            return ResponseEntity.status(404).body("لم يتم العثور على الملف أو لا يوجد عمال نشطين.");
        }
    }
}