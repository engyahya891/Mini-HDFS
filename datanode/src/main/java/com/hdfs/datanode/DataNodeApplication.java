package com.hdfs.datanode;

import com.hdfs.common.protocol.WorkerRegisterRequest;
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

    // لم نعد بحاجة لحقن الخدمات هنا لأنها ستقرأ من MasterContext تلقائياً

    public static void main(String[] args) {
        SpringApplication.run(DataNodeApplication.class, args);
    }

    @Override
    public void run(String... args) {

        System.out.println("=========================================");
        System.out.println("   HDFS DATA NODE (WORKER) STARTING...   ");
        System.out.println("=========================================");

        Scanner scanner = new Scanner(System.in);

        // البرنامج الآن يعمل، لكن الخدمات صامتة (Silent) لأن MasterContext فارغ
        System.out.print("✍️ Lütfen Master IP adresini girin (Enter = localhost): ");
        String masterIp = scanner.nextLine().trim();

        if (masterIp.isEmpty()) {
            masterIp = "localhost";
        }

        // بناء الرابط
        String fullUrl = "http://" + masterIp + ":8080";

        // 🟢 اللحظة الحاسمة: ضبط العنوان في الكلاس المشترك
        // بمجرد تنفيذ هذا السطر، ستبدأ الخدمات (Heartbeat & Report) بالعمل في دورتها القادمة
        MasterContext.set(fullUrl);

        System.out.println("🔗 Master URL global olarak ayarlandı: " + MasterContext.get());

        // --- محاولة التسجيل (Registration) مرة واحدة عند البدء ---
        String registerUrl = MasterContext.get() + "/api/worker/register";
        String storagePath = "./data/worker_" + serverPort;
        new File(storagePath).mkdirs();

        WorkerRegisterRequest request = new WorkerRegisterRequest();
        request.setPort(serverPort);
        request.setStoragePath(storagePath);

        RestTemplate restTemplate = new RestTemplate();
        System.out.println("📡 Register isteği gönderiliyor: " + registerUrl);

        try {
            restTemplate.postForObject(registerUrl, request, String.class);
            System.out.println("✅ Worker başarıyla register edildi.");
            System.out.println("🚀 Servisler (Heartbeat/BlockReport) şimdi veri göndermeye başlayacak.");
        } catch (Exception e) {
            System.out.println("❌ Kayıt başarısız (Master kapalı olabilir).");
            System.out.println("⚠️ Ancak servisler arka planda denemeye devam edecek.");
        }
    }
}