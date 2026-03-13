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
import java.util.Scanner;
import java.util.stream.Collectors;

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
            // هذا هو الحل السحري للمشكلة التي اكتشفتها! يبحث دائماً عن البلوكات الناقصة.
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

                                        // Bir sonrakine geçmeden önce biraz bekle (Ağı yormamak için)
                                        Thread.sleep(1000);
                                    } catch (Exception e) {
                                        System.out.println("❌ [Scanner] Kopya emri gönderilemedi: " + e.getMessage());
                                    }
                                } else {
                                    // Sadece debug için (Sürekli ekrana basmamak için kapatılabilir)
                                    // System.out.println("⏳ [Scanner] " + blockId + " için boşta uygun Worker bekleniyor...");
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

            // 3️⃣ Yönetici Kontrol Paneli (CLI) başlatılıyor ⌨️
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                System.out.println("\n========================================");
                System.out.println("   ANA DÜĞÜM YÖNETİCİ KONSOLU (HDFS)    ");
                System.out.println("========================================");
                System.out.println("Komutlar: list-workers, delete-worker <url>, exit");

                while (true) {
                    System.out.print("Yönetici> ");
                    String input = scanner.nextLine();
                    String[] parts = input.trim().split("\\s+");
                    if (parts.length == 0 || parts[0].isEmpty()) continue;
                    String command = parts[0].toLowerCase();

                    try {
                        switch (command) {
                            case "list-workers":
                                List<WorkerNode> list = workerRepository.findAll();
                                System.out.println("📋 Kayıtlı Çalışan Düğümler (" + list.size() + "):");

                                for (WorkerNode w : list) {
                                    String status = w.isActive() ? "🟢 ÇEVRİMİÇİ" : "🔴 ÇEVRİMDIŞI";
                                    long secondsAgo = Duration.between(w.getLastHeartbeat(), LocalDateTime.now()).getSeconds();

                                    System.out.println("\n   🌍 URL      : " + w.getUrl());
                                    System.out.println("      Durum    : " + status + " (Son aktivite: " + secondsAgo + " sn önce)");

                                    if (w.getStorageInfo() != null) {
                                        System.out.println("      Depolama : " + w.getStorageInfo());
                                    } else {
                                        long usedMB = w.getUsed() / (1024 * 1024);
                                        long capMB  = w.getCapacity() / (1024 * 1024);
                                        System.out.println("      Depolama : " + usedMB + " MB / " + capMB + " MB");
                                    }
                                }
                                break;

                            case "delete-worker":
                                if (parts.length < 2) {
                                    System.out.println("❌ Kullanım: delete-worker <url>");
                                } else {
                                    WorkerNode node = workerRepository.findByUrl(parts[1]);
                                    if (node != null) {
                                        workerRepository.delete(node);
                                        System.out.println("🗑️ Çalışan düğüm kayıttan silindi: " + parts[1]);
                                    } else {
                                        System.out.println("❌ Çalışan düğüm bulunamadı.");
                                    }
                                }
                                break;

                            case "exit":
                                System.out.println("👋 Konsol kapatılıyor...");
                                scanner.close();
                                System.exit(0);
                                return;

                            default:
                                System.out.println("❓ Bilinmeyen komut. Kullanılabilir komutlar: list-workers, delete-worker, exit");
                        }
                    } catch (Exception e) {
                        System.out.println("❌ Hata: " + e.getMessage());
                    }
                }
            }).start();
        };
    }
}