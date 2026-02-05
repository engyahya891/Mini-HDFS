package com.hdfs.namenode;

import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import com.hdfs.namenode.repository.BlockRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class NameNodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NameNodeApplication.class, args);
    }

    @Bean
    CommandLineRunner runSystem(WorkerRepository workerRepository,
                                BlockRepository blockRepository) {
        return args -> {

            // 1️⃣ Akıllı "Arıza Tespit Monitörü" başlatılıyor 🕵️‍♂️
            // Ping göndermek yerine sadece zaman aşımı kontrolü yapar
            new Thread(() -> {
                System.out.println("⏳ Arıza Tespit Monitörü Başlatıldı...");
                while (true) {
                    try {
                        Thread.sleep(5000); // Her 5 saniyede bir kontrol eder

                        List<WorkerNode> workers = workerRepository.findAll();
                        LocalDateTime now = LocalDateTime.now();

                        for (WorkerNode worker : workers) {
                            // Çalışan düğüm aktifse son heartbeat zamanını kontrol et
                            if (worker.isActive()) {
                                Duration duration =
                                        Duration.between(worker.getLastHeartbeat(), now);

                                // 15 saniyeden fazla heartbeat alınamadıysa düğüm ölü kabul edilir
                                if (duration.getSeconds() > 15) {
                                    worker.setActive(false);
                                    workerRepository.save(worker);
                                    System.out.println(
                                            "🔴 UYARI: Çalışan düğüm zaman aşımına uğradı (Timeout): "
                                                    + worker.getUrl()
                                    );
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("⚠️ Monitör Hatası: " + e.getMessage());
                    }
                }
            }).start();

            // 2️⃣ Yönetici Kontrol Paneli (CLI) başlatılıyor ⌨️
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
                    String command = parts[0].toLowerCase();

                    try {
                        switch (command) {
                            case "list-workers":
                                List<WorkerNode> list = workerRepository.findAll();
                                System.out.println(
                                        "📋 Kayıtlı Çalışan Düğümler (" + list.size() + "):"
                                );

                                for (WorkerNode w : list) {
                                    String status =
                                            w.isActive() ? "🟢 ÇEVRİMİÇİ" : "🔴 ÇEVRİMDIŞI";

                                    // Son heartbeat'ten bu yana geçen süre
                                    long secondsAgo =
                                            Duration.between(
                                                    w.getLastHeartbeat(),
                                                    LocalDateTime.now()
                                            ).getSeconds();

                                    System.out.println("\n   🌍 URL      : " + w.getUrl());
                                    System.out.println(
                                            "      Durum    : " + status +
                                                    " (Son aktivite: " + secondsAgo + " sn önce)"
                                    );

                                    // Heartbeat'ten gelen depolama bilgisi varsa göster
                                    if (w.getStorageInfo() != null) {
                                        System.out.println(
                                                "      Depolama : " + w.getStorageInfo()
                                        );
                                    } else {
                                        long usedMB = w.getUsed() / (1024 * 1024);
                                        long capMB  = w.getCapacity() / (1024 * 1024);
                                        System.out.println(
                                                "      Depolama : " +
                                                        usedMB + " MB / " + capMB + " MB"
                                        );
                                    }
                                }
                                break;

                            case "delete-worker":
                                if (parts.length < 2) {
                                    System.out.println(
                                            "❌ Kullanım: delete-worker <url>"
                                    );
                                } else {
                                    WorkerNode node =
                                            workerRepository.findByUrl(parts[1]);
                                    if (node != null) {
                                        workerRepository.delete(node);
                                        System.out.println(
                                                "🗑️ Çalışan düğüm kayıttan silindi: " + parts[1]
                                        );
                                    } else {
                                        System.out.println("❌ Çalışan düğüm bulunamadı.");
                                    }
                                }
                                break;

                            case "exit":
                                System.out.println("👋 Konsol kapatılıyor...");
                                scanner.close();
                                return;

                            default:
                                System.out.println(
                                        "❓ Bilinmeyen komut. Kullanılabilir komutlar: list-workers, delete-worker, exit"
                                );
                        }
                    } catch (Exception e) {
                        System.out.println("❌ Hata: " + e.getMessage());
                    }
                }
            }).start();
        };
    }
}