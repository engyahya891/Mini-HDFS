package com.hdfs.namenode;

import com.hdfs.namenode.model.WorkerNode;
import com.hdfs.namenode.repository.WorkerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class NameNodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NameNodeApplication.class, args);
    }

    @Bean
    CommandLineRunner runSystem(WorkerRepository workerRepository) {
        return args -> {
            // 1️⃣ إعداد Localhost الافتراضي (إذا لم يوجد وركرز)
            if (workerRepository.count() == 0) {
                System.out.println("⚙️ Sistem: Varsayılan Localhost Worker ekleniyor...");
                // نضيفه ونفترضه نشطاً في البداية
                workerRepository.save(new WorkerNode("http://localhost:8081"));
            }

            // 2️⃣ تشغيل "نظام نبضات القلب" (Heartbeat Monitor) في الخلفية 💓
            new Thread(() -> {
                RestTemplate restTemplate = new RestTemplate();
                while (true) {
                    try {
                        // انتظر 10 ثواني بين كل فحص
                        Thread.sleep(10000);

                        List<WorkerNode> workers = workerRepository.findAll();
                        if (workers.isEmpty()) continue; // لا يوجد أحد لفحصه

                        // System.out.println("💓 Heartbeat: Worker'lar kontrol ediliyor...");

                        for (WorkerNode worker : workers) {
                            boolean wasActive = worker.isActive();
                            boolean isAlive = false;

                            try {
                                // حاول الاتصال بـ /health
                                restTemplate.getForEntity(worker.getUrl() + "/api/data/health", String.class);
                                isAlive = true; // نجح الاتصال
                            } catch (Exception e) {
                                isAlive = false; // فشل الاتصال
                            }

                            // تحديث الحالة في قاعدة البيانات فقط إذا تغيرت
                            if (wasActive != isAlive) {
                                worker.setActive(isAlive);
                                workerRepository.save(worker);
                                if (isAlive) {
                                    System.out.println("🟢 Worker Geri Döndü (Back Online): " + worker.getUrl());
                                } else {
                                    System.out.println("🔴 Worker Çöktü/Kapandı (Offline): " + worker.getUrl());
                                }
                            }
                        }

                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        System.out.println("⚠️ Heartbeat Hatası: " + e.getMessage());
                    }
                }
            }).start();

            // 3️⃣ تشغيل "لوحة تحكم الأدمن" (Admin Console) ⌨️
            new Thread(() -> {
                Scanner scanner = new Scanner(System.in);
                System.out.println("\n========================================");
                System.out.println("   MASTER ADMIN CONSOLE (HDFS MANAGER)   ");
                System.out.println("========================================");
                System.out.println("Komutlar: list-workers, add-worker <url>, delete-worker <url>, exit");

                while (true) {
                    System.out.print("Admin> ");
                    String input = scanner.nextLine();
                    String[] parts = input.trim().split("\\s+");
                    String command = parts[0].toLowerCase();

                    try {
                        switch (command) {
                            case "list-workers":
                                List<WorkerNode> list = workerRepository.findAll();
                                System.out.println("📋 Worker Listesi (" + list.size() + "):");
                                for (WorkerNode w : list) {
                                    String status = w.isActive() ? "🟢 AKTİF" : "🔴 PASİF (Offline)";
                                    System.out.println("   - [" + w.getId() + "] " + w.getUrl() + " -> " + status);
                                }
                                break;

                            case "delete-worker":
                                if (parts.length < 2) {
                                    System.out.println("❌ URL gerekli!");
                                } else {
                                    WorkerNode node = workerRepository.findByUrl(parts[1]);
                                    if (node != null) {
                                        workerRepository.delete(node);
                                        System.out.println("🗑️ Silindi: " + parts[1]);
                                    } else {
                                        System.out.println("❌ Bulunamadı.");
                                    }
                                }
                                break;

                            case "exit":
                                return;
                        }
                    } catch (Exception e) {
                        System.out.println("Hata: " + e.getMessage());
                    }
                }
            }).start();
        };
    }
}