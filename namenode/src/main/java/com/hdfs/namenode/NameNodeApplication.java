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
            new Thread(() -> {
                System.out.println("⏳ Arıza Tespit Monitörü Başlatıldı... (İlk 30 saniye Güvenli Mod / Safe Mode)");

                // 🟢 تحديد وقت إقلاع الماستر لحساب فترة الوضع الآمن
                LocalDateTime startupTime = LocalDateTime.now();

                while (true) {
                    try {
                        Thread.sleep(5000); // Her 5 saniyede bir kontrol eder

                        LocalDateTime now = LocalDateTime.now();

                        // 🟢 فترة السماح (Safe Mode): تجاهل الفحص في أول 30 ثانية
                        if (Duration.between(startupTime, now).getSeconds() < 30) {
                            continue; // العودة لبداية الحلقة بصمت
                        }

                        List<WorkerNode> workers = workerRepository.findAll();

                        for (WorkerNode worker : workers) {
                            if (worker.isActive()) {
                                Duration duration = Duration.between(worker.getLastHeartbeat(), now);

                                // 15 saniyeden fazla heartbeat alınamadıysa düğüm ölü kabul edilir
                                if (duration.getSeconds() > 15) {
                                    worker.setActive(false);
                                    workerRepository.save(worker); // حفظ حالة الموت في الداتا بيز

                                    System.out.println("🔴 UYARI: Çalışan düğüm zaman aşımına uğradı (Timeout): " + worker.getUrl());

                                    // 🟢 هنا نطلق "الشفاء الذاتي" بمجرد اكتشاف الموت
                                    System.out.println("🔄 Otomatik replikasyon (Kurtarma) süreci başlatılıyor...");
                                    recoverDeadWorkerBlocks(worker, workerRepository, blockRepository);
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

    // 🚑 دالة الشفاء الذاتي: تعويض البلوكات المفقودة من العقدة الميتة (توضع هنا خارج الـ Bean وداخل الكلاس)
    private void recoverDeadWorkerBlocks(WorkerNode deadWorker, WorkerRepository workerRepo, BlockRepository blockRepo) {
        System.out.println("\n🔍 [Kurtarma] Ölü düğümdeki bloklar aranıyor: " + deadWorker.getUrl());

        // 1. ماذا خسرنا؟
        List<BlockMetadata> lostBlocks = blockRepo.findByWorker(deadWorker);

        if (lostBlocks.isEmpty()) {
            System.out.println("✅ [Kurtarma] Ölü düğümde kurtarılacak blok yok. Sistem güvende.");
            return;
        }

        List<WorkerNode> allWorkers = workerRepo.findAll();
        RestTemplate restTemplate = new RestTemplate();

        // 2. معالجة كل بلوك مفقود
        for (BlockMetadata lostBlock : lostBlocks) {
            String blockId = lostBlock.getBlockId();
            System.out.println("⚠️ [Kurtarma] Kayıp blok tespit edildi: " + blockId);

            // 3. البحث عن ناجي
            List<BlockMetadata> copies = blockRepo.findByBlockId(blockId);
            WorkerNode survivor = null;

            for (BlockMetadata copy : copies) {
                WorkerNode w = copy.getWorker();
                if (w.isActive() && !w.getUrl().equals(deadWorker.getUrl())) {
                    survivor = w;
                    break;
                }
            }

            if (survivor == null) {
                System.out.println("❌ [Kurtarma] KRİTİK HATA: " + blockId + " için hayatta kalan kopya bulunamadı! VERİ KAYBI!");
                continue;
            }

            // 4. البحث عن هدف
            WorkerNode target = null;
            for (WorkerNode w : allWorkers) {
                if (w.isActive() && !w.getUrl().equals(deadWorker.getUrl()) && !w.getUrl().equals(survivor.getUrl())) {
                    boolean hasBlock = copies.stream().anyMatch(c -> c.getWorker().getUrl().equals(w.getUrl()));
                    if (!hasBlock) {
                        target = w;
                        break;
                    }
                }
            }

            if (target == null) {
                System.out.println("⚠️ [Kurtarma] " + blockId + " için uygun hedef düğüm bulunamadı (Yeterli aktif Worker yok).");
                continue;
            }

            // 5. إرسال أمر النسخ الفعلي عبر الشبكة للناجي
            System.out.println("🚀 [Kurtarma] EMR: " + survivor.getUrl() + " -> " + target.getUrl() + " [" + blockId + "]");

            try {
                // نطلب من الناجي أن ينسخ الملف إلى الهدف
                String replicateUrl = survivor.getUrl() + "/api/file/replicate";

                // نرسل له اسم الملف وعنوان الهدف
                Map<String, String> requestData = new HashMap<>();
                requestData.put("blockId", blockId);
                requestData.put("targetUrl", target.getUrl());

                restTemplate.postForObject(replicateUrl, requestData, String.class);
                System.out.println("✅ [Kurtarma] Kopyalama emri başarıyla iletildi.");

            } catch (Exception e) {
                System.out.println("❌ [Kurtarma] Kopya emri gönderilemedi: " + e.getMessage());
            }
        }
    }
}