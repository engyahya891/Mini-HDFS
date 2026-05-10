package com.hdfs.namenode.controller;

import com.hdfs.namenode.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import com.hdfs.common.protocol.WorkerRegisterRequest;
import com.hdfs.common.protocol.StorageReportRequest;
import com.hdfs.common.protocol.HeartbeatRequest;
import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.BlockRepository;
import com.hdfs.namenode.repository.WorkerRepository;
import com.hdfs.namenode.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LogService logService; // 🟢 أضف هذا السطر

    // 🟢 ذاكرة مؤقتة لمنع تكرار إشعارات المساحة الممتلئة
    private static final Map<String, Boolean> storageWarningsSent = new ConcurrentHashMap<>();

    @PostMapping("/register")
    public ResponseEntity<String> registerWorker(
            @RequestBody WorkerRegisterRequest request,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }

        String workerUrl = "http://" + ipAddress + ":" + request.getPort();

        WorkerNode worker = workerRepository.findByUrl(workerUrl);
        if (worker == null) {
            worker = new WorkerNode(workerUrl);
            notificationService.addNotification(
                    "success",
                    "Yeni Düğüm Eklendi",
                    workerUrl + " adresli Worker kümeye başarıyla katıldı."
            );
        }

        worker.setStoragePath(request.getStoragePath());
        worker.setActive(true);
        worker.setLastHeartbeat(LocalDateTime.now());

        workerRepository.save(worker);

        // داخل دالة registerWorker
        logService.addLog("INFO", "NodeManager", workerUrl + " adresli yeni Worker kümeye katıldı.");

        return ResponseEntity.ok("Kümeye hoş geldiniz!");
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> receiveHeartbeat(@RequestBody HeartbeatRequest request) {
        WorkerNode worker = workerRepository.findByUrl(request.getWorkerId());
        if (worker == null) {
            worker = new WorkerNode(request.getWorkerId());
            worker.setStoragePath("Otomatik Algılandı");
        }
        // 🟢 تحديث بيانات النود بما فيها الرف
        worker.setRackId(request.getRackId() != null ? request.getRackId() : "Rack-1");

        worker.setActive(true);
        worker.setLastHeartbeat(LocalDateTime.now());
        worker.setUsed(request.getUsedSpace());
        worker.setCapacity(request.getTotalSpace());
        worker.setCpuUsage(request.getCpuUsage());
        worker.setRamUsage(request.getRamUsage());

        long usedMB = request.getUsedSpace() / (1024 * 1024);
        long totalMB = request.getTotalSpace() / (1024 * 1024);
        worker.setStorageInfo(usedMB + " MB / " + totalMB + " MB");

        // 🟢 منطق الإشعار الذكي (مرة واحدة فقط)
        if (request.getTotalSpace() > 0) {
            double usagePercentage = (double) request.getUsedSpace() / request.getTotalSpace();
            String workerId = request.getWorkerId();

            if (usagePercentage > 0.90) {
                // إذا لم نرسل تحذيراً من قبل، نرسل الآن
                if (!storageWarningsSent.getOrDefault(workerId, false)) {
                    // داخل شرط تجاوز المساحة 90%
                    logService.addLog("WARN", "StorageMonitor", request.getWorkerId() + " düğümünün depolama alanı kritik seviyede (>%90).");

                    notificationService.addNotification(
                            "warning",
                            "Kritik Depolama Alanı",
                            workerId + " düğümünün depolama alanı %90'ı aştı!"
                    );
                    storageWarningsSent.put(workerId, true); // نعلم النظام أننا أرسلنا
                }
            } else {
                // إذا انخفضت المساحة، نعيد تعيين التحذير لكي يرسل مستقبلاً إذا امتلأت مجدداً
                storageWarningsSent.put(workerId, false);
            }
        }

        workerRepository.save(worker);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/report")
    @Transactional
    public ResponseEntity<?> report(@RequestBody StorageReportRequest req) {
        WorkerNode worker = workerRepository.findByUrl(req.getWorkerUrl());
        if (worker == null) return ResponseEntity.badRequest().body("Bilinmeyen Worker");

        worker.setActive(true);
        worker.setLastHeartbeat(LocalDateTime.now());
        workerRepository.save(worker);

        blockRepository.deleteByWorker(worker);

        if (req.getBlockIds() != null) {
            for (String blockId : req.getBlockIds()) {
                BlockMetadata block = new BlockMetadata();
                block.setBlockId(blockId);
                block.setWorker(worker);
                block.setWorkerUrl(worker.getUrl());

                if (blockId.contains("_part_")) {
                    String realName = blockId.substring(0, blockId.lastIndexOf("_part_"));
                    block.setFilename(realName);
                } else {
                    block.setFilename(blockId);
                }
                blockRepository.save(block);
            }
        }
        return ResponseEntity.ok("Rapor güncellendi");
    }
}