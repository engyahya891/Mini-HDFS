package com.hdfs.namenode.controller;

import jakarta.servlet.http.HttpServletRequest;

import com.hdfs.common.protocol.WorkerRegisterRequest;
import com.hdfs.common.protocol.StorageReportRequest;
import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.BlockRepository;
import com.hdfs.namenode.repository.WorkerRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/worker")
public class WorkerController {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private BlockRepository blockRepository;

    /**
     * Worker Registration Endpoint
     * - Called once when DataNode starts
     * - Registers or re-activates the worker
     */
    @PostMapping("/register")
    public ResponseEntity<String> registerWorker(
            @RequestBody WorkerRegisterRequest request,
            HttpServletRequest servletRequest) {

        // 1️⃣ Get IP address dynamically from the request
        String ipAddress = servletRequest.getRemoteAddr();

        // Fix IPv6 localhost for local testing
        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }

        // 2️⃣ Build worker URL using real IP + port sent by DataNode
        String workerUrl = "http://" + ipAddress + ":" + request.getPort();

        System.out.println("🔔 Worker register request from: " + workerUrl);

        // 3️⃣ Check if worker already exists
        WorkerNode worker = workerRepository.findByUrl(workerUrl);

        if (worker == null) {
            worker = new WorkerNode(workerUrl);
            System.out.println("🆕 New worker registered: " + workerUrl);
        } else {
            System.out.println("♻️ Worker re-joined: " + workerUrl);
        }

        // 4️⃣ Update worker metadata
        worker.setStoragePath(request.getStoragePath());
        worker.setActive(true);
        worker.setLastHeartbeat(LocalDateTime.now());

        workerRepository.save(worker);

        return ResponseEntity.ok("Worker registered successfully");
    }

    /**
     * Storage + Block Report Endpoint
     * - Called periodically by DataNode
     * - Updates storage info and block metadata
     */
    @PostMapping("/report")
    public ResponseEntity<?> report(
            @RequestBody StorageReportRequest req,
            HttpServletRequest servletRequest
    ) {
        String ip = servletRequest.getRemoteAddr();

        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        // نفترض أن كل Worker معروف مسبقًا بنفس الـ IP
        String urlPrefix = "http://" + ip + ":";

        var workers = workerRepository.findByUrlStartingWith(urlPrefix);

        if (workers.isEmpty()) {
            return ResponseEntity.badRequest().body("Worker not registered");
        }

// إذا عندك Worker واحد لكل IP (وضعك الحالي)
        WorkerNode worker = workers.get(0);


        if (worker == null) {
            return ResponseEntity.badRequest().body("Worker not registered");
        }

        worker.setCapacity(req.getCapacity());
        worker.setUsed(req.getUsed());
        worker.setActive(true);
        worker.setLastHeartbeat(LocalDateTime.now());
        workerRepository.save(worker);

        blockRepository.deleteByWorker(worker);

        for (String blockId : req.getBlockIds()) {
            BlockMetadata block = new BlockMetadata();
            block.setBlockId(blockId);
            block.setWorker(worker);
            block.setWorkerUrl(worker.getUrl());
            blockRepository.save(block);
        }

        return ResponseEntity.ok("Report updated");
    }

}
