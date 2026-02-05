package com.hdfs.namenode.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.hdfs.common.protocol.WorkerRegisterRequest;
import com.hdfs.common.protocol.StorageReportRequest;
import com.hdfs.common.protocol.HeartbeatRequest;
import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.BlockRepository;
import com.hdfs.namenode.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BlockRepository blockRepository;

    @PostMapping("/register")
    public ResponseEntity<String> registerWorker(
            @RequestBody WorkerRegisterRequest request,
            HttpServletRequest servletRequest) {

        String ipAddress = servletRequest.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }

        String workerUrl = "http://" + ipAddress + ":" + request.getPort();
        System.out.println("🔔 Katılma isteği alındı: " + workerUrl);

        WorkerNode worker = workerRepository.findByUrl(workerUrl);
        if (worker == null) {
            worker = new WorkerNode(workerUrl);
            System.out.println("🆕 Yeni worker kümeye katıldı: " + workerUrl);
        }

        worker.setStoragePath(request.getStoragePath());
        worker.setActive(true);
        worker.setLastHeartbeat(LocalDateTime.now());
        workerRepository.save(worker);

        return ResponseEntity.ok("Kümeye hoş geldiniz!");
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Void> receiveHeartbeat(@RequestBody HeartbeatRequest request) {

        WorkerNode worker = workerRepository.findByUrl(request.getWorkerId());
        if (worker == null) {
            worker = new WorkerNode(request.getWorkerId());
            worker.setStoragePath("Otomatik Algılandı");
        }

        worker.setActive(true);
        worker.setLastHeartbeat(LocalDateTime.now());
        worker.setUsed(request.getUsedSpace());
        worker.setCapacity(request.getTotalSpace());

        long usedMB = request.getUsedSpace() / (1024 * 1024);
        long totalMB = request.getTotalSpace() / (1024 * 1024);
        worker.setStorageInfo(usedMB + " MB / " + totalMB + " MB");

        workerRepository.save(worker);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/report")
    @Transactional
    public ResponseEntity<?> report(@RequestBody StorageReportRequest req) {

        WorkerNode worker = workerRepository.findByUrl(req.getWorkerUrl());
        if (worker == null) {
            return ResponseEntity.badRequest().body("Bilinmeyen Worker");
        }

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

        System.out.println(
                "✅ Rapor işlendi: " + worker.getUrl() +
                        " (" + (req.getBlockIds() != null ? req.getBlockIds().size() : 0) + " blok)"
        );

        return ResponseEntity.ok("Rapor güncellendi");
    }
}
