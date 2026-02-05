package com.hdfs.datanode;

import com.hdfs.common.protocol.WorkerRegisterRequest;
import com.hdfs.datanode.service.BlockReportService; // 👈 تأكد من الاستيراد
import com.hdfs.datanode.service.HeartbeatService;   // 👈 تأكد من الاستيراد
import org.springframework.beans.factory.annotation.Autowired; // 👈 تأكد من الاستيراد
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.InetAddress;
import java.util.Scanner;

@SpringBootApplication
@EnableScheduling
public class DataNodeApplication implements CommandLineRunner {

    @Value("${server.port}")
    private int serverPort;

    // 🟢 1. حقن الخدمات هنا لنتمكن من تعديل إعداداتها
    @Autowired
    private HeartbeatService heartbeatService;

    @Autowired
    private BlockReportService blockReportService;

    public static void main(String[] args) {
        SpringApplication.run(DataNodeApplication.class, args);
    }

    @Override
    public void run(String... args) {

        System.out.println("=========================================");
        System.out.println("   HDFS DATA NODE (WORKER) STARTING...   ");
        System.out.println("=========================================");

        Scanner scanner = new Scanner(System.in);

        System.out.print("✍️ Lütfen Master IP adresini girin (Enter = localhost): ");
        String masterIp = scanner.nextLine().trim();

        if (masterIp.isEmpty()) {
            masterIp = "localhost";
        }

        // بناء الرابط الأساسي للماستر
        String masterUrl = "http://" + masterIp + ":8080";

        // 🟢 2. الخطوة الحاسمة: تحديث الخدمات بالرابط الجديد
        // الآن Heartbeat و Report سيرسلان إلى IP زميلك وليس localhost
        heartbeatService.setMasterUrl(masterUrl);
        blockReportService.setMasterUrl(masterUrl);

        System.out.println("🔗 Master URL ayarlandı: " + masterUrl);

        // إكمال عملية التسجيل
        String registerUrl = masterUrl + "/api/worker/register";
        String storagePath = "./data/worker_" + serverPort;
        new File(storagePath).mkdirs();

        WorkerRegisterRequest request = new WorkerRegisterRequest();
        request.setPort(serverPort);
        request.setStoragePath(storagePath);

        try {
            String myIp = InetAddress.getLocalHost().getHostAddress();
            System.out.println("ℹ️ My Detected IP: " + myIp);
        } catch (Exception e) {
            // تجاهل الخطأ
        }

        RestTemplate restTemplate = new RestTemplate();
        System.out.println("📡 Register isteği gönderiliyor: " + registerUrl);

        try {
            restTemplate.postForObject(registerUrl, request, String.class);
            System.out.println("✅ Worker başarıyla register edildi.");
            System.out.println("💓 Heartbeat ve BlockReport servisleri güncellendi ve çalışıyor...");
        } catch (Exception e) {
            System.out.println("❌ Master'a bağlanılamadı!");
            System.out.println("Sebep: " + e.getMessage());
            // لا نغلق البرنامج لنسمح للخدمات بالمحاولة المستمرة
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n⚠️ Worker kapanıyor...");
        }));
    }
}