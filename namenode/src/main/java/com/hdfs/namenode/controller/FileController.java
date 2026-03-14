package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.FileMetadata;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.BlockRepository;
import com.hdfs.namenode.repository.FileRepository;
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
import java.net.URLEncoder;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private FileRepository fileRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // 🟢 1. الرفع (Allocate Block)
    @PostMapping("/allocate-block")
    public ResponseEntity<BlockAllocation> allocateBlock(
            @RequestBody BlockAllocation requestInfo,
            @RequestParam(name = "owner", defaultValue = "anonymous") String owner,
            @RequestParam(name = "filename") String filename) {

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

        try {
            if (filename != null && !filename.isEmpty()) {
                FileMetadata fileMeta = fileRepository.findByFilename(filename);

                if (fileMeta == null) {
                    // 🟢 التعديل الجوهري: ننشئ الملف فقط إذا كان هذا هو البلوك الأول
                    if (requestInfo.getBlockIndex() == 1) {
                        fileMeta = new FileMetadata(filename, 0, owner);
                        fileRepository.save(fileMeta);
                        System.out.println("📝 Yeni dosya kaydedildi: " + filename + " kullanıcı: " + owner);
                    } else {
                        // 🔴 إذا لم يكن البلوك الأول والملف غير موجود، فهذا يعني أنه حُذف أثناء الرفع!
                        System.out.println("🚨 HATA: Dosya yükleme sırasında silinmiş! İşlem reddedildi.");
                        return ResponseEntity.status(409).build(); // 409 Conflict
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ hata saving file metadata: " + e.getMessage());
        }

        BlockAllocation response = new BlockAllocation();
        response.setBlockIndex(requestInfo.getBlockIndex());
        response.setWorkerUrls(selectedUrls);

        return ResponseEntity.ok(response);
    }

    // 🟢 2. القائمة (List)
    @GetMapping("/list/{owner}")
    public ResponseEntity<List<String>> listFiles(@PathVariable String owner) {
        System.out.println("📂 Dosya listeleme isteği: " + owner);
        List<FileMetadata> userFiles = fileRepository.findByOwner(owner);
        List<String> fileNames = userFiles.stream()
                .map(FileMetadata::getFilename)
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(fileNames);
    }

    // 🟢 3. البحث (Download Location)
    @GetMapping("/locate/{filename:.+}")
    public ResponseEntity<String> locateFile(
            @PathVariable String filename,
            @RequestParam(name = "owner") String owner) {

        System.out.println("🔍 araştırma talebi : " + filename);

        // المحاولة الأولى: البحث بالاسم كما وصل
        FileMetadata fileMeta = fileRepository.findByFilename(filename);

        // المحاولة الثانية: إذا فشل، نحاول فك التشفير (للمسافات والأحرف الخاصة)
        if (fileMeta == null) {
            try {
                String decodedName = URLDecoder.decode(filename, StandardCharsets.UTF_8.toString());
                fileMeta = fileRepository.findByFilename(decodedName);
            } catch (Exception e) {}
        }

        if (fileMeta == null || !fileMeta.getOwner().equals(owner)) {
            return ResponseEntity.status(404).body("DOSYA_BULUNAMADI");
        }

        String workerUrl = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .findFirst()
                .map(WorkerNode::getUrl)
                .orElse(null);

        if (workerUrl == null) {
            return ResponseEntity.status(503).body("WORKER_YOK");
        }

        return ResponseEntity.ok(workerUrl);
    }

    // 🔴 4. الحذف (Delete) - النسخة المدرعة والمحمية ضد إحياء البيانات
    @Transactional // 🟢 إضافة حاسمة لضمان تنفيذ مسح قاعدة البيانات كعملية واحدة (Atomic)
    @DeleteMapping("/delete/{filename:.+}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String filename,
            @RequestParam(name = "owner") String owner) {

        System.out.println("🗑️ Master: Silme isteği -> " + filename + " (" + owner + ")");

        // 1. البحث عن الملف
        FileMetadata fileMeta = fileRepository.findByFilename(filename);

        if (fileMeta == null) {
            try {
                String decodedName = URLDecoder.decode(filename, StandardCharsets.UTF_8.toString());
                fileMeta = fileRepository.findByFilename(decodedName);
                if (fileMeta != null) {
                    System.out.println("✅ Dosya decode edilerek bulundu: " + decodedName);
                }
            } catch (Exception e) {}
        }

        if (fileMeta == null) {
            System.out.println("❌ HATA: Dosya veritabanında bulunamadı!");
            return ResponseEntity.status(404).body("HATA: Dosya bulunamadı!");
        }

        if (!fileMeta.getOwner().equals(owner)) {
            return ResponseEntity.status(403).body("HATA: Bu dosyayı silmeye yetkiniz yok!");
        }

        String realFilename = fileMeta.getFilename();

        try {
            // 🟢 التعديل المعماري: مسح البلوكات (الأبناء) أولاً لتجنب إحياء الكائنات
            List<BlockMetadata> allBlocks = blockRepository.findAll();
            for (BlockMetadata block : allBlocks) {
                if (Objects.equals(block.getFilename(), realFilename) ||
                        (block.getBlockId() != null && block.getBlockId().startsWith(realFilename))) {
                    blockRepository.delete(block);
                }
            }

            // 🟢 ضربة قاضية: مسح الملف الأصلي (الأب) باستخدام ID مباشرة
            fileRepository.deleteById(realFilename);

            // 2. إرسال أمر الحذف للـ Workers
            List<WorkerNode> workers = workerRepository.findAll();
            String base64Name = Base64.getUrlEncoder().encodeToString(realFilename.getBytes(StandardCharsets.UTF_8));

            for (WorkerNode worker : workers) {
                if (worker.isActive()) {
                    try {
                        String workerDeleteUrl = worker.getUrl() + "/api/data/delete/" + base64Name;
                        restTemplate.delete(workerDeleteUrl);
                        System.out.println("📤 Silme isteği Worker'a gönderildi : " + worker.getUrl());
                    } catch (Exception ignored) {
                        System.out.println("⚠️ Worker Erişilemiyor: " + worker.getUrl());
                    }
                }
            }

            return ResponseEntity.ok("Dosya başarıyla silindi.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Veritabanı hatası: " + e.getMessage());
        }
    }

    // 🟢 دالة جديدة: البحث عن عنوان Worker يملك "جزء معين (Block)" وليس الملف بالكامل
    @GetMapping("/locate-block/{blockId:.+}")
    public ResponseEntity<String> locateBlock(@PathVariable String blockId) {
        try {
            // فك التشفير
            String decodedBlockId = URLDecoder.decode(blockId, StandardCharsets.UTF_8.toString());

            // نبحث في الداتا بيز عن كل النسخ الخاصة بهذا الجزء
            List<BlockMetadata> blocks = blockRepository.findByBlockId(decodedBlockId);

            // نمر عليها ونعيد عنوان أول Worker "نشط" يملك هذا الجزء
            for (BlockMetadata block : blocks) {
                if (block.getWorker().isActive()) {
                    return ResponseEntity.ok(block.getWorker().getUrl());
                }
            }

            // إذا لم نجد الجزء (يعني وصلنا لنهاية الملف) نعيد 404
            return ResponseEntity.status(404).body("BLOK_BULUNAMADI");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("HATA: " + e.getMessage());
        }
    }
}