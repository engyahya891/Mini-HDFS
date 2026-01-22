package com.hdfs.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.Scanner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

    public static void main(String[] args) {
        // نغلق الويب سيرفر لأن العميل لا يحتاج أن يستقبل طلبات، هو فقط يرسل
        SpringApplication.run(ClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Mini-HDFS istemcisine Hoş Geldiniz !");
        System.out.println("Kullanılabilir komutlar : upload <dosya_yolu>, download <dosya_adı> ,delete <dosya_adı> ,exit");

        while (true) {
            System.out.print("> ");
            String commandLine = scanner.nextLine().trim(); // نقرأ السطر كاملاً

            if (commandLine.isEmpty()) continue;

            // تقسيم النص إلى قسمين فقط:
            // 1. الأمر (upload/download)
            // 2. كل ما تبقى من السطر (المسار مهما كان فيه فراغات)
            String[] parts = commandLine.split("\\s+", 2);

            String command = parts[0];

            if ("exit".equalsIgnoreCase(command)) {
                break;
            } else if ("upload".equalsIgnoreCase(command)) {
                if (parts.length < 2) {
                    System.out.println("Hata : Lütfen bir Dosya yolu girin !!");
                    continue;
                }
                String filePath = parts[1];
                uploadFile(filePath);
            } else if ("download".equalsIgnoreCase(command)) {
                if (parts.length < 2) {
                    System.out.println("⚠️ Lütfen dosya adını girin (Please enter filename).");
                    continue;
                }
                String filename = parts[1];
                downloadFile(filename); // سنقوم بإنشاء هذه الدالة الآن
            }else {
                System.out.println("Bilinmeyen Komut.");
            }
        }
    }

    // هنا سنكتب كود الاتصال بالماستر لاحقاً
    // الخطوة القادمة:
    // 1. سؤال الماستر: أين أرفع هذا الملف؟
    // 2. تقطيع الملف.
    // 3. الإرسال للووركر.
    // نحتاج هذه المكتبة لإرسال الطلبات

    private final org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

    private void uploadFile(String path) {
        java.io.File file = new java.io.File(path);
        if (!file.exists()) {
            System.out.println("❌ Hata: Dosya bulunamadı! (Error: File not found)");
            return;
        }

        System.out.println("🔄 Master sunucusuna bağlanılıyor... (Connecting to Master...)");

        // 1. طلب الإذن من الماستر (كما فعلنا سابقاً)
        com.hdfs.common.protocol.ClientUploadRequest request =
                new com.hdfs.common.protocol.ClientUploadRequest(file.getName(), file.length());

        try {
            String masterUrl = "http://localhost:8080/api/file/upload";
            com.hdfs.common.protocol.ClientUploadResponse response =
                    restTemplate.postForObject(masterUrl, request, com.hdfs.common.protocol.ClientUploadResponse.class);

            if (response != null && response.isSuccess()) {
                System.out.println("✅ Master onayı alındı! (Master approved)");
                String workerUrl = response.getWorkerUrl(); // سيأتي الرابط http://localhost:8081

                // 2. البدء بإرسال الملف للووركر
                System.out.println("🚀 Dosya Worker'a gönderiliyor... (Sending file to Worker...)");

                // تجهيز الملف للإرسال
                org.springframework.core.io.FileSystemResource fileResource = new org.springframework.core.io.FileSystemResource(file);

                // تجهيز الهيدر والبيانات (Multipart Request)
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

                org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
                body.add("file", fileResource);

                org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity =
                        new org.springframework.http.HttpEntity<>(body, headers);

                // الإرسال الفعلي
                String uploadUrl = workerUrl + "/api/data/write";
                String result = restTemplate.postForObject(uploadUrl, requestEntity, String.class);

                System.out.println("🏁 Sonuç: " + result); // طباعة النتيجة النهائية

            } else {
                System.out.println("⛔ Master isteği reddetti.");
            }

        } catch (Exception e) {
            System.out.println("❌ Hata oluştu: " + e.getMessage());
        }
    }


    private void downloadFile(String filename) {
        System.out.println("🔄 Master'a soruluyor... (Asking Master...)");

        try {
            // 1. نسأل الماستر: أين الملف؟ (سيجلب المعلومة من الداتا بيس الجديدة)
            String masterUrl = "http://localhost:8080/api/file/locate/" + filename;
            String workerUrl = restTemplate.getForObject(masterUrl, String.class);

            if ("NOT_FOUND".equals(workerUrl)) {
                System.out.println("⛔ Dosya Master kayıtlarında yok! (File not found)");
                return;
            }

            System.out.println("📍 Dosya bulundu: " + workerUrl);
            System.out.println("⬇️ İndiriliyor... (Downloading...)");

            // 2. نحمل الملف من الووركر
            String downloadUrl = workerUrl + "/api/data/read/" + filename;
            byte[] fileBytes = restTemplate.getForObject(downloadUrl, byte[].class);

            // 3. نحفظ الملف عندنا باسم جديد
            String savePath = "downloaded_" + filename;
            java.nio.file.Files.write(java.nio.file.Paths.get(savePath), fileBytes);

            System.out.println("🎉 İndirme başarılı! Dosya: " + savePath);

        } catch (Exception e) {
            System.out.println("❌ Hata: " + e.getMessage());
        }
    }
}
