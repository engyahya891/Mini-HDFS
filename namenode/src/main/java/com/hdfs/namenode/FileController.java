package com.hdfs.namenode;

import com.hdfs.common.protocol.BlockAllocation;
import com.hdfs.common.protocol.ClientUploadRequest;
import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.FileMetadata;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.FileRepository;
import com.hdfs.namenode.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private WorkerRepository workerRepository;

    // 🟢 تعريف حجم البلوك (64 ميجابايت)
    private static final long BLOCK_SIZE = 64 * 1024 * 1024;

    // متغير لتطبيق الدور (Round Robin)
    private int currentWorkerIndex = 0;

    // --- دالة الرفع المعدلة (ذكية وتقوم بالتقطيع) ---
    @PostMapping("/upload")
    public List<BlockAllocation> handleUploadRequest(@RequestBody ClientUploadRequest request) {

        System.out.println("📥 طلب رفع جديد (موزع): " + request.getFilename() + " | الحجم: " + request.getFileSize());

        // 1. التحقق من وجود الووركرز النشطين
        List<WorkerNode> activeWorkers = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .toList();

        if (activeWorkers.isEmpty()) {
            throw new RuntimeException("❌ لا يوجد ووركرز متاحين للنظام!");
        }

        // 2. حساب عدد البلوكات المطلوبة
        // معادلة السقف: (Size + BlockSize - 1) / BlockSize
        int totalBlocks = (int) Math.ceil((double) request.getFileSize() / BLOCK_SIZE);
        if (totalBlocks == 0) totalBlocks = 1; // للملفات الفارغة جداً

        System.out.println("🔢 سيتم تقسيم الملف إلى: " + totalBlocks + " بلوكات.");

        // 3. إنشاء سجل الملف (لاحظ: لم نعد نمرر workerUrl هنا)
        FileMetadata fileData = new FileMetadata(request.getFilename(), request.getFileSize());

        // القائمة التي سنرسلها للعميل (خطة التوزيع)
        List<BlockAllocation> responsePlan = new ArrayList<>();

        // 4. حلقة توزيع البلوكات (The Logic) 🔄
        for (int i = 0; i < totalBlocks; i++) {

            // اختيار الووركر بالدور
            WorkerNode selectedWorker = activeWorkers.get(currentWorkerIndex % activeWorkers.size());
            currentWorkerIndex++; // زيادة العداد للمرة القادمة

            String targetWorkerUrl = selectedWorker.getUrl();

            // أ) إنشاء البلوك وربطه بالملف (للداتا بيس)
            BlockMetadata block = new BlockMetadata(i, targetWorkerUrl, fileData);
            fileData.addBlock(block);

            // ب) إضافة للخطة (للعميل)
            responsePlan.add(new BlockAllocation(i, targetWorkerUrl));

            System.out.println("   🔸 بلوك #" + i + " -> " + targetWorkerUrl);
        }

        // 5. حفظ الملف مع بلوكاته (Cascade Save)
        // إذا كان الملف موجوداً مسبقاً، سنحذفه وننشئ جديداً (أو يمكنك منع التكرار)
        if (fileRepository.existsById(request.getFilename())) {
            fileRepository.deleteById(request.getFilename());
        }
        fileRepository.save(fileData);

        // إرجاع الخطة للعميل
        return responsePlan;
    }

    // --- دالة البحث (Locate) ---
    // هذه الدالة تحتاج تحديثاً بسيطاً لترجع قائمة البلوكات، لكن مؤقتاً
    // سنجعلها ترجع مكان "أول بلوك" فقط لكي لا نغير الكثير في وقت واحد
    // أو يمكنك تركها كما هي إذا كنت لا تستخدمها حالياً للتحميل الموزع
    @GetMapping("/locate/{filename}")
    public String locateFile(@PathVariable String filename) {
        Optional<FileMetadata> fileData = fileRepository.findById(filename);

        if (fileData.isPresent() && !fileData.get().getBlocks().isEmpty()) {
            // نرجع رابط الووركر الذي يملك البلوك رقم 0
            return fileData.get().getBlocks().get(0).getWorkerUrl();
        } else {
            return "NOT_FOUND";
        }
    }

    // --- دالة الحذف ---
    // ⚠️ ملاحظة: دالة الحذف تحتاج تحديثاً كبيراً لتمسح كل البلوكات
    // سأقوم بتعليقها مؤقتاً لتجنب الأخطاء حتى ننتهي من الرفع
    /*
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        // ... يحتاج لمنطق جديد لحذف البلوكات المتعددة ...
        return ResponseEntity.ok("سيتم التحديث لاحقاً");
    }
    */
}
