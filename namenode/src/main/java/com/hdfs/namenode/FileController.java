package com.hdfs.namenode;

import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import com.hdfs.common.protocol.BlockAllocation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private WorkerRepository workerRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // 🟢 1. Blok Tahsis Etme (Upload işlemi için)
    @PostMapping("/allocate-block")
    public ResponseEntity<BlockAllocation> allocateBlock(@RequestBody BlockAllocation requestInfo) {

        System.out.println("📦 Master: Blok #" + requestInfo.getBlockIndex() + " için yer tahsis isteği alındı.");

        // Aktif olan Worker'ları getir
        List<WorkerNode> activeWorkers = workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .collect(Collectors.toList());

        if (activeWorkers.isEmpty()) {
            System.out.println("❌ HATA: Aktif Worker bulunamadı!");
            return ResponseEntity.status(500).build();
        }

        // Yük dengeleme (Load Balancing) için listeyi karıştır
        Collections.shuffle(activeWorkers);

        // Replikasyon faktörü (Örn: 2 kopya)
        int replicationFactor = Math.min(activeWorkers.size(), 2);

        // Uygun Worker URL'lerini seç
        List<String> selectedWorkerUrls = activeWorkers.stream()
                .limit(replicationFactor)
                .map(WorkerNode::getUrl)
                .collect(Collectors.toList());

        System.out.println("   ✅ Blok şuraya yönlendirildi: " + selectedWorkerUrls);

        BlockAllocation response = new BlockAllocation();
        response.setBlockIndex(requestInfo.getBlockIndex());
        response.setWorkerUrls(selectedWorkerUrls);

        return ResponseEntity.ok(response);
    }

    // 🟢 2. Dosya Konumunu Belirle (Download işlemi için)
    @GetMapping("/locate/{filename}")
    public String locateFile(@PathVariable String filename) {
        // İlk aktif worker'ı döndür (Basit model)
        return workerRepository.findAll().stream()
                .filter(WorkerNode::isActive)
                .findFirst()
                .map(WorkerNode::getUrl)
                .orElse("DOSYA_BULUNAMADI");
    }

    // 🟢 3. Küresel Silme (Global Delete) ✅
    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        System.out.println("🗑️ Master: Dosya silme komutu yayılıyor -> " + filename);

        List<WorkerNode> workers = workerRepository.findAll();
        int successCount = 0;

        // Dosya ismini URL uyumlu hale getir (Boşluklar ve özel karakterler için)
        String encodedFilename;
        try {
            encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            encodedFilename = filename;
        }

        for (WorkerNode worker : workers) {
            if (worker.isActive()) {
                try {
                    // Worker'ın silme API'sine istek at
                    String workerDeleteUrl = worker.getUrl() + "/api/data/delete/" + encodedFilename;
                    restTemplate.delete(workerDeleteUrl);

                    System.out.println("   ✅ Worker'dan silindi: " + worker.getUrl());
                    successCount++;
                } catch (Exception e) {
                    System.out.println("   ⚠️ Worker silme hatası: " + worker.getUrl() + " - " + e.getMessage());
                }
            }
        }

        if (successCount > 0) {
            // İstemciye (Client) null dönmemesi için anlamlı bir mesaj veriyoruz
            return ResponseEntity.ok("Dosya başarıyla sistemden ve tüm düğümlerden temizlendi.");
        } else {
            return ResponseEntity.status(404).body("Dosya bulunamadı veya aktif Worker yok.");
        }
    }
}