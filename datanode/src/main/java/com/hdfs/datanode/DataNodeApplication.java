package com.hdfs.datanode;

import com.hdfs.common.protocol.WorkerRegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.Scanner;

@SpringBootApplication
public class DataNodeApplication implements CommandLineRunner {

    @Value("${server.port}")
    private int serverPort;

    public static void main(String[] args) {
        SpringApplication.run(DataNodeApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=========================================");
        System.out.println("   HDFS DATA NODE (WORKER) STARTING...   ");
        System.out.println("=========================================");

        // 1. طلب عنوان الماستر من المستخدم مباشرة
        Scanner scanner = new Scanner(System.in);
        System.out.println("✍️ Lütfen Master IP adresini girin (Örnek: 192.168.1.10): ");
        String masterIp = scanner.nextLine().trim();

        // إذا ضغط المستخدم Enter بدون كتابة، نستخدم Localhost كقيمة افتراضية
        if (masterIp.isEmpty()) {
            masterIp = "localhost";
            System.out.println("⚠️ Varsayılan olarak 'localhost' kullanılıyor.");
        }

        // 2. تجهيز الرابط والمسار
        String masterUrl = "http://" + masterIp + ":8080/api/worker/register";
        String dynamicStoragePath = "./data/worker_" + serverPort;
        new File(dynamicStoragePath).mkdirs();

        WorkerRegisterRequest request = new WorkerRegisterRequest();
        request.setPort(serverPort);
        request.setStoragePath(dynamicStoragePath);

        // 3. محاولة الاتصال
        RestTemplate restTemplate = new RestTemplate();
        System.out.println("📡 Bağlanılıyor: " + masterUrl);

        try {
            // نستخدم String.class لتجنب مشكلة الـ Void التي ظهرت سابقاً
            restTemplate.postForObject(masterUrl, request, String.class);
            System.out.println("✅ BAŞARILI! Master ile bağlantı kuruldu.");
        } catch (Exception e) {
            System.out.println("❌ HATA: Master'a ulaşılamadı! (" + e.getMessage() + ")");
        }
    }
}