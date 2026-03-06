package com.hdfs.datanode;

import com.hdfs.common.protocol.WorkerRegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

@SpringBootApplication
@EnableScheduling
public class DataNodeApplication implements CommandLineRunner {

    @Value("${server.port}")
    private int serverPort;

    private static final String CONFIG_FILE = "last_master_config.txt";

    public static void main(String[] args) {
        SpringApplication.run(DataNodeApplication.class, args);
    }

    // دالة لاسترجاع الـ IP المحفوظ
    private String loadLastMasterIp() {
        try {
            File file = new File(CONFIG_FILE);
            if (file.exists()) {
                String ip = new String(Files.readAllBytes(Paths.get(CONFIG_FILE))).trim();
                if (!ip.isEmpty()) return ip;
            }
        } catch (Exception e) {}
        return null;
    }

    // دالة لحفظ الـ IP
    private void saveMasterIp(String ip) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            writer.write(ip);
        } catch (Exception e) {}
    }

    // دالة لانتظار المستخدم 3 ثوانٍ (مع إمكانية المقاطعة)
    private boolean waitForUserInterrupt(int seconds) {
        try {
            long end = System.currentTimeMillis() + (seconds * 1000L);
            while (System.currentTimeMillis() < end) {
                if (System.in.available() > 0) {
                    System.in.read(new byte[System.in.available()]); // تنظيف مدخلات لوحة المفاتيح
                    return true; // المستخدم ضغط على زر
                }
                Thread.sleep(100);
            }
        } catch (Exception e) {}
        return false; // انتهى الوقت ولم يضغط المستخدم على شيء
    }

    @Override
    public void run(String... args) {

        System.out.println("=========================================");
        System.out.println("   HDFS DATA NODE (WORKER) STARTING...   ");
        System.out.println("=========================================");

        String masterIp = null;
        String savedIp = loadLastMasterIp();
        Scanner scanner = new Scanner(System.in);
        RestTemplate restTemplate = new RestTemplate();

        // تجهيز بيانات التسجيل (Registration)
        String storagePath = "./data/worker_" + serverPort;
        new File(storagePath).mkdirs();
        WorkerRegisterRequest request = new WorkerRegisterRequest();
        request.setPort(serverPort);
        request.setStoragePath(storagePath);

        boolean useManual = true; // متغير يحدد هل سنحتاج للإدخال اليدوي أم لا

        // 🟢 إذا كان هناك IP محفوظ، نبدأ الذكاء الاصطناعي الخاص بنا
        if (savedIp != null) {
            System.out.println("💡 Kayıtlı Master IP bulundu: [" + savedIp + "]");
            System.out.println("⏳ 3 saniye içinde otomatik bağlanılacak... (İptal edip yeni IP girmek için ENTER'a basın)");

            boolean interrupted = waitForUserInterrupt(3);

            if (!interrupted) {
                System.out.println("\n🔄 Otomatik bağlanılıyor...");
                String testUrl = "http://" + savedIp + ":8080/api/worker/register";
                try {
                    restTemplate.postForObject(testUrl, request, String.class);
                    System.out.println("✅ Otomatik bağlantı başarılı!");
                    masterIp = savedIp;
                    useManual = false; // نجحنا، لا حاجة للسؤال اليدوي
                } catch (Exception e) {
                    // 🛑 هنا الـ Fallback: فشل الاتصال، نعود للوضع اليدوي بهدوء
                    System.out.println("❌ Kayıtlı IP'ye bağlanılamadı (" + savedIp + "). Sunucu kapalı veya IP değişmiş olabilir.");
                }
            } else {
                System.out.println("\n🛑 Otomatik bağlantı iptal edildi.");
            }
        }

        // 🟠 الوضع اليدوي (يعمل إذا لم يكن هناك IP، أو إذا فشل التلقائي، أو إذا ألغاه المستخدم)
        if (useManual) {
            System.out.print("✍️ Lütfen Master IP adresini girin (Enter = localhost): ");
            masterIp = scanner.nextLine().trim();
            if (masterIp.isEmpty()) {
                masterIp = "localhost";
            }

            String registerUrl = "http://" + masterIp + ":8080/api/worker/register";
            System.out.println("📡 Register isteği gönderiliyor: " + registerUrl);
            try {
                restTemplate.postForObject(registerUrl, request, String.class);
                System.out.println("✅ Worker başarıyla register edildi.");
            } catch (Exception e) {
                System.out.println("❌ Kayıt başarısız (Master kapalı olabilir).");
                System.out.println("⚠️ Ancak servisler arka planda denemeye devam edecek.");
            }
        }


        // 🔵 في النهاية، نضبط الإعدادات ونحفظ الـ IP للمرة القادمة
        String fullUrl = "http://" + masterIp + ":8080";
        MasterContext.set(fullUrl);
        saveMasterIp(masterIp); // حفظ الـ IP الناجح في الملف

        System.out.println("🔗 Master URL global olarak ayarlandı: " + MasterContext.get());
        System.out.println("🚀 Servisler (Heartbeat/BlockReport) şimdi veri göndermeye başlayacak.");
    }
}