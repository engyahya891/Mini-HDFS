package com.hdfs.namenode.controller;

import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.BlockRepository;
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

    private final RestTemplate restTemplate = new RestTemplate();

    // 🟢 1. Blok ayırma (Upload)
    @PostMapping("/allocate-block")
    public ResponseEntity<BlockAllocation> allocateBlock(@RequestBody BlockAllocation requestInfo) {

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

        BlockAllocation response = new BlockAllocation();
        response.setBlockIndex(requestInfo.getBlockIndex());
        response.setWorkerUrls(selectedUrls);

        return ResponseEntity.ok(response);
    }

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

        System.out.println("🗑️ Master: Silme isteği alındı -> " + filename);

        // Veritabanı temizleme
        try {
            String cleanFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            List<BlockMetadata> allBlocks = blockRepository.findAll();
            int dbDeleted = 0;

            for (BlockMetadata block : allBlocks) {
                if (Objects.equals(block.getFilename(), cleanFilename) ||
                        (block.getBlockId() != null && block.getBlockId().startsWith(cleanFilename))) {
                    blockRepository.delete(block);
                    dbDeleted++;
                }
            }

            System.out.println("✅ Veritabanından " + dbDeleted + " kayıt silindi.");
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

                    String workerDeleteUrl =
                            worker.getUrl() + "/api/data/delete/" + base64Name;

                    restTemplate.delete(workerDeleteUrl);
                } catch (Exception ignored) {
                    // Worker kapalı olabilir → görmezden gel
                }
            }
        }

        return ResponseEntity.ok("Silme komutu gönderildi.");
    }

    // 🆕 4. Dosya listesi (LS Command)
    @GetMapping("/list")
    public ResponseEntity<List<String>> listFiles() {

        List<BlockMetadata> blocks = blockRepository.findAll();

        List<String> fileNames = blocks.stream()
                .map(BlockMetadata::getFilename)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return ResponseEntity.ok(fileNames);
    }
}
