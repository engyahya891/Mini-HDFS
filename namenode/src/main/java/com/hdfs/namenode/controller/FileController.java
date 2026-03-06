package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.FileMetadata; // 🟢 استيراد الكلاس الجديد
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.BlockRepository;
import com.hdfs.namenode.repository.FileRepository; // 🟢 استيراد الريبوزيتوري الجديد
import com.hdfs.namenode.repository.WorkerRepository;
import com.hdfs.common.protocol.BlockAllocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.net.URLDecoder;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private FileRepository fileRepository; // 🟢 1. أضفنا هذا للتعامل مع جدول الملفات والمالك

    private final RestTemplate restTemplate = new RestTemplate();

    // 🟢 2. تعديل الرفع (Allocate Block) ليحفظ اسم المالك
    // أصبحنا نطلب "owner" كـ Parameter في الرابط
    // 🟢 2. تعديل الرفع (Allocate Block)
    // الآن نستقبل المالك + اسم الملف من الرابط مباشرة
    @PostMapping("/allocate-block")
    public ResponseEntity<BlockAllocation> allocateBlock(
            @RequestBody BlockAllocation requestInfo,
            @RequestParam(name = "owner", defaultValue = "anonymous") String owner,
            @RequestParam(name = "filename") String filename) { // 👈 الإضافة الجديدة هنا

        // 1. اختيار الـ Workers (نفس الكود القديم)
        List<WorkerNode> activeWorkers = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .collect(Collectors.toList());

        if (activeWorkers.isEmpty()) {
            return ResponseEntity.status(500).build();
        }

        Collections.shuffle(activeWorkers);
        int replicationFactor = Math.min(activeWorkers.size(), 2);

        List<String> selectedUrls = activeWorkers.stream()
                .limit(replicationFactor)
                .map(WorkerNode::getUrl)
                .collect(Collectors.toList());

        // 2. 🟢 حفظ الملكية (تم إصلاح الخطأ هنا)
        try {
            // لم نعد نستخدم requestInfo.getFilename()
            // بل نستخدم المتغير filename الذي وصلنا من الرابط مباشرة
            if (filename != null && !filename.isEmpty()) {
                FileMetadata fileMeta = fileRepository.findByFilename(filename);
                if (fileMeta == null) {
                    fileMeta = new FileMetadata(filename, 0, owner);
                    fileRepository.save(fileMeta);
                    System.out.println("📝 Yeni dosya kaydedildi: " + filename + " kullanıcı: " + owner);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ hata saving file metadata: " + e.getMessage());
        }

        // 3. تجهيز الرد
        BlockAllocation response = new BlockAllocation();
        response.setBlockIndex(requestInfo.getBlockIndex());
        response.setWorkerUrls(selectedUrls);

        return ResponseEntity.ok(response);
    }

    // 🟢 3. تعديل القائمة (List) لتعرض ملفات المالك فقط
    // الرابط الجديد أصبح: /api/file/list/{owner}
    @GetMapping("/list/{owner}")
    public ResponseEntity<List<String>> listFiles(@PathVariable String owner) {

        System.out.println("📂 Dosya listeleme isteği: " + owner);

        // البحث باستخدام الدالة التي أضفناها في FileRepository
        List<FileMetadata> userFiles = fileRepository.findByOwner(owner);

        List<String> fileNames = userFiles.stream()
                .map(FileMetadata::getFilename)
                .sorted()
                .collect(Collectors.toList());

        if (fileNames.isEmpty()) {
            System.out.println("ℹ️ Bu kullanıcı için dosya bulunamadı.");
        }

        return ResponseEntity.ok(fileNames);
    }

    // --- الدوال القديمة (Locate & Delete) تبقى كما هي مؤقتاً ---

    // 🟢 2. Dosya konumu bulma (Download)
    @GetMapping("/locate/{filename}")
    public String locateFile(@PathVariable String filename) {
        return workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .findFirst()
                .map(WorkerNode::getUrl)
                .orElse("DOSYA_BULUNAMADI");
    }

    // 🔴 3. Silme (Delete)
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        // ... (نفس كود الحذف القديم الخاص بك تماماً) ...
        // (اختصاراً للمساحة لم أعد كتابته، لكن اتركه كما هو عندك)
        System.out.println("🗑️ Master: Silme isteği alındı -> " + filename);

        try {
            String cleanFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);

            // 🟢 تحديث بسيط: حذف الميتاداتا الخاصة بالملف أيضاً
            FileMetadata fileMeta = fileRepository.findByFilename(cleanFilename);
            if (fileMeta != null) {
                fileRepository.delete(fileMeta);
            }

            // حذف البلوكات القديم
            List<BlockMetadata> allBlocks = blockRepository.findAll();
            for (BlockMetadata block : allBlocks) {
                if (Objects.equals(block.getFilename(), cleanFilename) ||
                        (block.getBlockId() != null && block.getBlockId().startsWith(cleanFilename))) {
                    blockRepository.delete(block);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Veritabanı hatası: " + e.getMessage());
        }

        // Fiziksel silme (Worker'lara gönder)
        List<WorkerNode> workers = workerRepository.findAll();
        for (WorkerNode worker : workers) {
            if (worker.isActive()) {
                try {
                    String base64Name = Base64.getUrlEncoder()
                            .encodeToString(filename.getBytes(StandardCharsets.UTF_8));
                    String workerDeleteUrl = worker.getUrl() + "/api/data/delete/" + base64Name;
                    restTemplate.delete(workerDeleteUrl);
                } catch (Exception ignored) {}
            }
        }
        return ResponseEntity.ok("Silme komutu gönderildi.");
    }
}