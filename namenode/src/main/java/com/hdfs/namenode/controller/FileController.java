package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.FileMetadata;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.BlockRepository;
import com.hdfs.namenode.repository.FileRepository;
import com.hdfs.namenode.repository.WorkerRepository;
import com.hdfs.namenode.service.LogService;
import com.hdfs.namenode.service.NotificationService;
import com.hdfs.common.protocol.BlockAllocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.net.URLDecoder;
import org.springframework.transaction.annotation.Transactional;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LogService logService; // 🟢 أضف هذا السطر

    private final RestTemplate restTemplate = new RestTemplate();

    // 🟢 دالة مساعدة لفك التشفير الإجباري
    private String decodeFilename(String filename) {
        try {
            return URLDecoder.decode(filename, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            return filename;
        }
    }

    @PostMapping("/allocate-block")
    public ResponseEntity<BlockAllocation> allocateBlock(
            @RequestBody BlockAllocation requestInfo,
            @RequestParam(name = "owner", defaultValue = "anonymous") String owner,
            @RequestParam(name = "filename") String filename) {

        // 🟢 إصلاح جذري: فك التشفير قبل فعل أي شيء
        String realFilename = decodeFilename(filename);

        List<WorkerNode> activeWorkers = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .collect(Collectors.toList());

        if (activeWorkers.isEmpty()) return ResponseEntity.status(500).build();

        Collections.shuffle(activeWorkers);
        int replicationFactor = Math.min(activeWorkers.size(), 2);

        List<String> selectedUrls = activeWorkers.stream()
                .limit(replicationFactor)
                .map(WorkerNode::getUrl)
                .collect(Collectors.toList());

        try {
            if (realFilename != null && !realFilename.isEmpty()) {
                FileMetadata fileMeta = fileRepository.findByFilename(realFilename);

                if (fileMeta == null) {
                    if (requestInfo.getBlockIndex() == 1) {
                        // حفظ الاسم العربي الحقيقي والنظيف
                        fileMeta = new FileMetadata(realFilename, 0, owner);
                        fileRepository.save(fileMeta);
                    } else {
                        return ResponseEntity.status(409).build();
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

    @GetMapping("/list/{owner}")
    public ResponseEntity<List<String>> listFiles(@PathVariable String owner) {
        List<FileMetadata> userFiles = fileRepository.findByOwner(owner);
        List<String> fileNames = userFiles.stream()
                .map(FileMetadata::getFilename)
                .sorted()
                .collect(Collectors.toList());
        return ResponseEntity.ok(fileNames);
    }

    @GetMapping("/locate/{filename:.+}")
    public ResponseEntity<String> locateFile(
            @PathVariable String filename,
            @RequestParam(name = "owner") String owner) {

        String realFilename = decodeFilename(filename);
        FileMetadata fileMeta = fileRepository.findByFilename(realFilename);

        if (fileMeta == null || !fileMeta.getOwner().equals(owner)) {
            return ResponseEntity.status(404).body("DOSYA_BULUNAMADI");
        }

        String workerUrl = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .findFirst()
                .map(WorkerNode::getUrl)
                .orElse(null);

        if (workerUrl == null) return ResponseEntity.status(503).body("WORKER_YOK");

        return ResponseEntity.ok(workerUrl);
    }

    @Transactional
    @DeleteMapping("/delete/{filename:.+}")
    public ResponseEntity<String> deleteFile(
            @PathVariable String filename,
            @RequestParam(name = "owner") String owner) {

        String realFilename = decodeFilename(filename);
        FileMetadata fileMeta = fileRepository.findByFilename(realFilename);

        if (fileMeta == null) {
            return ResponseEntity.status(404).body("HATA: Dosya bulunamadı!");
        }

        if (!fileMeta.getOwner().equals(owner)) {
            return ResponseEntity.status(403).body("HATA: Bu dosyayı silmeye yetkiniz yok!");
        }

        try {
            List<BlockMetadata> allBlocks = blockRepository.findAll();
            for (BlockMetadata block : allBlocks) {
                if (Objects.equals(block.getFilename(), realFilename) ||
                        (block.getBlockId() != null && block.getBlockId().startsWith(realFilename))) {
                    blockRepository.delete(block);
                }
            }

            fileRepository.deleteById(realFilename);

            List<WorkerNode> workers = workerRepository.findAll();
            String base64Name = Base64.getUrlEncoder().encodeToString(realFilename.getBytes(StandardCharsets.UTF_8));

            for (WorkerNode worker : workers) {
                if (worker.isActive()) {
                    try {
                        String workerDeleteUrl = worker.getUrl() + "/api/data/delete/" + base64Name;
                        restTemplate.delete(workerDeleteUrl);
                    } catch (Exception ignored) {}
                }
            }
            // عند نجاح حذف الملف
            logService.addLog("WARN", "FileSystem", owner + " kullanıcısı '" + realFilename + "' adlı dosyayı sistemden sildi.");

            // 🟢 إرسال إشعار حذف الملف
            notificationService.addNotification(
                    "warning",
                    "Dosya Silindi",
                    owner + " kullanıcısı '" + realFilename + "' adlı dosyayı sistemden sildi."
            );

            return ResponseEntity.ok("Dosya başarıyla silindi.");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Veritabanı hatası: " + e.getMessage());
        }
    }

    @GetMapping("/locate-block/{blockId:.+}")
    public ResponseEntity<String> locateBlock(@PathVariable String blockId) {
        try {
            String decodedBlockId = decodeFilename(blockId);
            List<BlockMetadata> blocks = blockRepository.findByBlockId(decodedBlockId);

            for (BlockMetadata block : blocks) {
                if (block.getWorker().isActive()) {
                    return ResponseEntity.ok(block.getWorker().getUrl());
                }
            }
            return ResponseEntity.status(404).body("BLOK_BULUNAMADI");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("HATA: " + e.getMessage());
        }
    }

    @PostMapping("/update-checksum")
    public ResponseEntity<String> updateChecksum(
            @RequestParam String filename,
            @RequestParam String owner,
            @RequestParam long fileSize,
            @RequestParam String checksum) {

        // 🟢 فك التشفير ليتمكن من العثور على الملف الصحيح
        String realFilename = decodeFilename(filename);
        FileMetadata fileMeta = fileRepository.findByFilename(realFilename);

        if (fileMeta != null && fileMeta.getOwner().equals(owner)) {
            fileMeta.setFileSize(fileSize);
            fileMeta.setMd5Checksum(checksum);
            fileRepository.save(fileMeta);

            // عند نجاح تحديث البصمة (اكتمال الرفع)
            logService.addLog("INFO", "FileSystem", owner + " kullanıcısı '" + realFilename + "' adlı dosyayı başarıyla yükledi.");

            // 🟢 إرسال إشعار نجاح الرفع بالاسم العربي الصحيح!
            notificationService.addNotification(
                    "success",
                    "Yeni Dosya Yüklendi",
                    owner + " kullanıcısı '" + realFilename + "' adlı dosyayı sisteme başarıyla yükledi."
            );

            return ResponseEntity.ok("OK");
        }
        return ResponseEntity.status(404).body("Dosya bulunamadı veya yetkisiz.");
    }

    @GetMapping("/info/{filename:.+}")
    public ResponseEntity<Map<String, Object>> getFileInfo(
            @PathVariable String filename,
            @RequestParam String owner) {

        try {
            String decodedName = decodeFilename(filename);
            FileMetadata fileMeta = fileRepository.findByFilename(decodedName);

            if (fileMeta == null || !fileMeta.getOwner().equals(owner)) {
                return ResponseEntity.status(404).body(null);
            }

            Map<String, Object> info = new HashMap<>();
            info.put("filename", fileMeta.getFilename());
            info.put("size", fileMeta.getFileSize());
            info.put("checksum", fileMeta.getMd5Checksum() != null ? fileMeta.getMd5Checksum() : "YOK");

            List<BlockMetadata> allBlocks = blockRepository.findAll().stream()
                    .filter(b -> b.getBlockId() != null && b.getBlockId().startsWith(decodedName))
                    .collect(Collectors.toList());

            // 🟢 التعديل السحري: استخدام Set بدلاً من List لمنع التكرار نهائياً!
            Map<String, Set<String>> blockLocations = new TreeMap<>();
            for (BlockMetadata block : allBlocks) {
                String bId = block.getBlockId();
                String workerInfo = block.getWorker().getUrl() + (block.getWorker().isActive() ? " (Aktif)" : " (ÖLÜ)");

                // Set سيتجاهل أي عامل مكرر ولن يعرضه إلا مرة واحدة فقط في الـ X-Ray
                blockLocations.computeIfAbsent(bId, k -> new LinkedHashSet<>()).add(workerInfo);
            }

            info.put("blocksCount", blockLocations.size());
            info.put("locations", blockLocations);

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
    @GetMapping("/all-files-info")
    public ResponseEntity<List<Map<String, Object>>> getAllFilesForDashboard() {
        try {
            List<FileMetadata> allFiles = fileRepository.findAll();
            List<Map<String, Object>> responseList = new ArrayList<>();
            List<BlockMetadata> allBlocks = blockRepository.findAll();

            for (FileMetadata fileMeta : allFiles) {
                Map<String, Object> fileData = new HashMap<>();
                fileData.put("id", fileMeta.getFilename());
                fileData.put("filename", fileMeta.getFilename());
                fileData.put("size", fileMeta.getFileSize());
                fileData.put("owner", fileMeta.getOwner());

                long blocksCount = allBlocks.stream()
                        .filter(b -> b.getBlockId() != null && b.getBlockId().startsWith(fileMeta.getFilename()))
                        .count();
                fileData.put("nodes", blocksCount > 0 ? blocksCount : 1);

                fileData.put("uploadedAt", fileMeta.getUploadedAt() != null ? fileMeta.getUploadedAt().toString() : java.time.LocalDateTime.now().toString());

                responseList.add(fileData);
            }

            return ResponseEntity.ok(responseList);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}