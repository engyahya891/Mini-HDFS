package com.hdfs.namenode;

import com.hdfs.namenode.model.BlockMetadata;
import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import com.hdfs.namenode.repository.BlockRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors; // تم إبقاء هذا
// تم حذف import java.util.Scanner; لأننا لم نعد نحتاجها هنا

@SpringBootApplication
public class NameNodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NameNodeApplication.class, args);
    }

    @Bean
    CommandLineRunner runSystem(WorkerRepository workerRepository,
                                BlockRepository blockRepository) {
        return args -> {

            // 1️⃣ Akıllı "Arıza Tespit Monitörü" başlatılıyor 🕵️‍♂️ (Sadece ölüleri tespit eder)
            new Thread(() -> {
                System.out.println("⏳ Arıza Tespit Monitörü Başlatıldı...");
                LocalDateTime startupTime = LocalDateTime.now();

                while (true) {
                    try {
                        Thread.sleep(5000); // Her 5 saniyede bir kontrol
                        LocalDateTime now = LocalDateTime.now();

                        // 🟢 İlk 30 saniye Safe Mode
                        if (Duration.between(startupTime, now).getSeconds() < 30) {
                            continue;
                        }

                        List<WorkerNode> workers = workerRepository.findAll();
                        for (WorkerNode worker : workers) {
                            if (worker.isActive()) {
                                Duration duration = Duration.between(worker.getLastHeartbeat(), now);

                                // 15 saniyeden fazla heartbeat alınamadıysa düğüm ölü kabul edilir
                                if (duration.getSeconds() > 15) {
                                    worker.setActive(false);
                                    workerRepository.save(worker); // حفظ حالة الموت
                                    System.out.println("🔴 UYARI: Çalışan düğüm zaman aşımına uğradı (Timeout): " + worker.getUrl());
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Monitör Hatası: " + e.getMessage());
                    }
                }
            }).start();

            // 2️⃣ 🟢 YENİ: Global Replikasyon Tarayıcısı (Scanner) 🔄
            new Thread(() -> {
                System.out.println("🔍 Global Replikasyon Tarayıcısı Başlatıldı... (Eksik kopyaları arayacak)");
                RestTemplate restTemplate = new RestTemplate();

                while (true) {
                    try {
                        Thread.sleep(15000); // Her 15 saniyede bir tüm sistemi tarar

                        List<WorkerNode> activeWorkers = workerRepository.findAll().stream()
                                .filter(WorkerNode::isActive)
                                .collect(Collectors.toList());

                        // Eğer 2'den az aktif worker varsa, kopyalama yapılamaz (bekle)
                        if (activeWorkers.size() < 2) continue;

                        // 1. Sistemdeki tüm benzersiz blok ID'lerini bul
                        List<String> uniqueBlockIds = blockRepository.findAll().stream()
                                .map(BlockMetadata::getBlockId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList());

                        // 2. Her bir blok için kopya sayısını kontrol et
                        for (String blockId : uniqueBlockIds) {
                            List<BlockMetadata> copies = blockRepository.findByBlockId(blockId);

                            // Bu bloğa sahip olan "Aktif" worker'ları bul
                            List<WorkerNode> workersHavingBlock = copies.stream()
                                    .map(BlockMetadata::getWorker)
                                    .filter(WorkerNode::isActive)
                                    .collect(Collectors.toList());

                            int activeCopyCount = workersHavingBlock.size();

                            // Eğer 1 kopya varsa (veya hedeflenen 2 kopyadan azsa), replikasyon gerekir!
                            if (activeCopyCount > 0 && activeCopyCount < 2) {
                                System.out.println("\n⚠️ [Scanner] Eksik kopya tespit edildi: " + blockId + " (Mevcut aktif kopya: " + activeCopyCount + ")");

                                WorkerNode source = workersHavingBlock.get(0); // Hayatta kalan düğüm
                                WorkerNode target = null; // Yeni eklenecek hedef düğüm

                                // Hedef düğümü seç (bu bloğa sahip olmayan aktif bir düğüm olmalı)
                                for (WorkerNode w : activeWorkers) {
                                    boolean hasBlock = workersHavingBlock.stream().anyMatch(active -> active.getUrl().equals(w.getUrl()));
                                    if (!hasBlock) {
                                        target = w;
                                        break;
                                    }
                                }

                                if (target != null) {
                                    System.out.println("🚀 [Scanner] EMR: " + source.getUrl() + " -> " + target.getUrl() + " [" + blockId + "]");
                                    try {
                                        String replicateUrl = source.getUrl() + "/api/file/replicate";
                                        Map<String, String> requestData = new HashMap<>();
                                        requestData.put("blockId", blockId);
                                        requestData.put("targetUrl", target.getUrl());

                                        restTemplate.postForObject(replicateUrl, requestData, String.class);
                                        System.out.println("✅ [Scanner] Kopyalama emri başarıyla iletildi.");

                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        System.out.println("❌ [Scanner] Kopya emri gönderilemedi: " + e.getMessage());
                                    }
                                }
                            } else if (activeCopyCount == 0) {
                                System.out.println("💀 [Scanner] KRİTİK VERİ KAYBI: " + blockId + " için hiç aktif kopya kalmadı!");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Scanner Hatası: " + e.getMessage());
                    }
                }
            }).start();

            // 🛑 تم حذف شاشة الأوامر (CLI) من هنا بناءً على طلبك
            System.out.println("✅ NameNode başarıyla başlatıldı. Konsol yönetimi artık İstemci (Client) üzerinden yapılmaktadır.");
        };
    }
}