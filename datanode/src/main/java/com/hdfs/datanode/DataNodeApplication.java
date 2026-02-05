package com.hdfs.datanode;

import com.hdfs.common.protocol.WorkerRegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // 🟢 1. استيراد هام
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.InetAddress;
import java.util.Scanner;

@SpringBootApplication
@EnableScheduling // 🟢 2. تفعيل الجدولة (ضروري لكي يعمل HeartbeatService)
public class DataNodeApplication implements CommandLineRunner {

    @Value("${server.port}")
    private int serverPort;

    public static void main(String[] args) {
        SpringApplication.run(DataNodeApplication.class, args);
    }

    @Override
    public void run(String... args) {

        System.out.println("=========================================");
        System.out.println("   HDFS DATA NODE (WORKER) STARTING...   ");
        System.out.println("=========================================");

        Scanner scanner = new Scanner(System.in);

        // التحسين: جعلنا localhost هو الافتراضي لتسهيل التجربة
        System.out.print("✍️ Lütfen Master IP adresini girin (Enter = localhost): ");
        String masterIp = scanner.nextLine().trim();

        if (masterIp.isEmpty()) {
            masterIp = "localhost";
        }

        String registerUrl = "http://" + masterIp + ":8080/api/worker/register";
        String storagePath = "./data/worker_" + serverPort;
        new File(storagePath).mkdirs();

        WorkerRegisterRequest request = new WorkerRegisterRequest();
        request.setPort(serverPort);
        request.setStoragePath(storagePath);

        // 🟢 3. تحسين ذكي: محاولة جلب IP الجهاز الحقيقي بدلاً من الاعتماد على التخمين
        try {
            String myIp = InetAddress.getLocalHost().getHostAddress();
            // إذا كنت تشغل الوركر على نفس جهاز الماستر، اتركه localhost
            // لكن إذا كان جهازاً منفصلاً، الماستر يحتاج الـ IP الحقيقي
            System.out.println("ℹ️ My Detected IP: " + myIp);
            // (اختياري: يمكنك إرسال هذا الـ IP للماستر لو عدلنا WorkerRegisterRequest لاحقاً)
        } catch (Exception e) {
            // تجاهل الخطأ
        }

        RestTemplate restTemplate = new RestTemplate();
        System.out.println("📡 Bağlanılıyor: " + registerUrl);

        try {
            restTemplate.postForObject(registerUrl, request, String.class);
            System.out.println("✅ Worker başarıyla register edildi.");
            System.out.println("💓 Heartbeat servisi arka planda çalışıyor..."); // رسالة تأكيد
        } catch (Exception e) {
            System.out.println("❌ Master'a bağlanılamadı!");
            System.out.println("Sebep: " + e.getMessage());
            // لا نغلق البرنامج (System.exit) لأننا نريد أن يستمر Heartbeat بالمحاولة
            // System.exit(1);
        }

        // هوك الإغلاق (كما هو)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠️ Worker kapanıyor...");
        }));
    }
}