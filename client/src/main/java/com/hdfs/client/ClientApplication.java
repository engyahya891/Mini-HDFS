package com.hdfs.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

    // 🟢 متغيرات لضبط عنوان الماستر دينامikياً
    private static String MASTER_IP = "localhost";
    private static String MASTER_URL = "http://" + MASTER_IP + ":8080";

    private final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        // 🟢 1. إعداد الاتصال عند البداية
        System.out.println("⚙️  HDFS Client Configuration");
        System.out.print("✍️  Lütfen Master IP adresini girin (Default: localhost): ");
        String inputIp = scanner.nextLine().trim();

        if (!inputIp.isEmpty()) {
            MASTER_IP = inputIp;
            MASTER_URL = "http://" + MASTER_IP + ":8080";
        }

        System.out.println("✅ Master adresi ayarlandı: " + MASTER_URL);
        System.out.println("------------------------------------------------");
        System.out.println("Mini-HDFS istemcisine Hoş Geldiniz !");
        System.out.println("Kullanılabilir komutlar : upload <dosya>, download <dosya>, delete <dosya>, clear, exit");

        while (true) {
            System.out.print("> ");
            String commandLine = scanner.nextLine().trim();

            if (commandLine.isEmpty()) continue;

            String[] parts = commandLine.split("\\s+", 2);
            String command = parts[0];

            if ("exit".equalsIgnoreCase(command)) {
                System.out.println("Güle güle! Çıkış yapılıyor...");
                break;
            }

            if ("upload".equalsIgnoreCase(command)) {
                if (parts.length < 2) {
                    System.out.println("⚠️ Lütfen dosya yolunu belirtin.");
                    continue;
                }
                uploadFile(removeQuotes(parts[1]));

            } else if ("download".equalsIgnoreCase(command)) {
                if (parts.length < 2) {
                    System.out.println("⚠️ Lütfen dosya adını belirtin.");
                    continue;
                }
                downloadFile(removeQuotes(parts[1]));

            } else if ("delete".equalsIgnoreCase(command)) {
                if (parts.length < 2) {
                    System.out.println("⚠️ Lütfen silinecek dosya adını belirtin.");
                    continue;
                }
                deleteFileRequest(removeQuotes(parts[1]));

            } else if ("clear".equalsIgnoreCase(command)) {
                clearScreen();
                System.out.println("✨ Console Cleared! ✨");
            } else {
                System.out.println("Bilinmeyen komut. (Yardım: upload, download, delete, clear, exit)");
            }
        }
    }

    private String removeQuotes(String path) {
        path = path.trim();
        if (path.startsWith("\"") && path.endsWith("\"")) {
            return path.substring(1, path.length() - 1);
        }
        return path;
    }



    // --- دالة الرفع (Upload) المعدلة بدقة ---
    private void uploadFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("❌ Dosya bulunamadı: " + path);
            return;
        }

        long fileSize = file.length();
        long blockSize = 64 * 1024 * 1024; // 64 MB
        // حساب عدد البلوكات بدقة باستخدام دالة السقف
        int totalBlocks = (int) Math.ceil((double) fileSize / blockSize);

        System.out.println("📦 Dosya " + totalBlocks + " bloğa ayrılıyor...");

        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[(int) blockSize];
            int bytesRead;
            int blockIndex = 1; // نبدأ الترقيم من 1 ليتوافق مع حلقة التحميل

            while ((bytesRead = fis.read(buffer)) != -1) {
                System.out.println("\n🔹 Blok numarası işleniyor: " + blockIndex + " / " + totalBlocks);

                // 1. طلب تخصيص من الماستر للقطعة الحالية
                com.hdfs.common.protocol.BlockAllocation request = new com.hdfs.common.protocol.BlockAllocation();
                request.setBlockIndex(blockIndex);
                // ملاحظة: تأكد أن الماستر يستقبل اسم الملف أيضاً لربط البلوكات به
                // request.setFileName(file.getName());

                String allocateUrl = MASTER_URL + "/api/file/allocate-block";
                com.hdfs.common.protocol.BlockAllocation response = restTemplate.postForObject(
                        allocateUrl, request, com.hdfs.common.protocol.BlockAllocation.class);

                if (response == null || response.getWorkerUrls() == null || response.getWorkerUrls().isEmpty()) {
                    System.out.println("❌ Başarısız: Master'dan uygun Worker adresi alınamadı.");
                    break;
                }

                // 2. تجهيز البيانات الفعلية فقط (تجنب إرسال 64 ميجا كاملة إذا كان الجزء الأخير أصغر)
                byte[] exactData = java.util.Arrays.copyOf(buffer, bytesRead);

                // 3. إرسال النسخ للـ Workers المحددين (Replication)
                for (String workerUrl : response.getWorkerUrls()) {
                    System.out.println("🚀 Kopya yükleniyor: " + workerUrl);
                    try {
                        // تعريف ملف افتراضي ليتمكن الـ Worker من قراءته كـ MultipartFile
                        final String partName = file.getName() + "_part_" + blockIndex;

                        org.springframework.core.io.ByteArrayResource byteResource = new org.springframework.core.io.ByteArrayResource(exactData) {
                            @Override
                            public String getFilename() {
                                return partName; // هذا الاسم هو ما سيخزن به الملف عند الـ Worker
                            }
                        };

                        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

                        org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
                        body.add("file", byteResource);

                        org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                                new org.springframework.http.HttpEntity<>(body, headers);

                        // إرسال البيانات للـ DataNode
                        restTemplate.postForObject(workerUrl + "/api/data/write", entity, String.class);
                        System.out.println("      ✅ " + partName + " başarıyla yüklendi.");

                    } catch (Exception e) {
                        System.out.println("      ⚠️ " + workerUrl + " adresine yükleme hatası: " + e.getMessage());
                    }
                }
                blockIndex++; // زيادة العداد للقطعة التالية
            }
            System.out.println("\n🎉 Tüm bloklar ve yedekleri başarıyla tamamlandı!");

        } catch (Exception e) {
            System.out.println("❌ Yükleme sırasında kritik hata: " + e.getMessage());
        }
    }

    // --- دالة التحميل (Download) مع التجميع (Reassembly) ---
    private void downloadFile(String filename) {
        System.out.println("🔄 Master'a soruluyor...");
        String targetFolder = "C:\\HDFS_Downloads\\";

        try {
            // 1. استعلام الماستر عن الـ Worker (يُفضل مستقبلاً استرجاع عدد الأجزاء أيضاً)
            String locateUrl = MASTER_URL + "/api/file/locate/" + filename;
            String workerUrl = restTemplate.getForObject(locateUrl, String.class);

            if (workerUrl == null || workerUrl.equals("DOSYA_BULUNAMADI")) {
                System.out.println("⛔ Dosya bulunamadı!");
                return;
            }

            File finalFile = new File(targetFolder + filename);
            try (FileOutputStream fos = new FileOutputStream(finalFile)) {
                int blockIndex = 1;
                boolean moreParts = true;

                while (moreParts) {
                    String partName = filename + "_part_" + blockIndex;
                    String downloadUrl = workerUrl + "/api/data/read/" + partName;

                    try {
                        // محاولة تحميل الجزء
                        org.springframework.http.ResponseEntity<byte[]> response =
                                restTemplate.getForEntity(downloadUrl, byte[].class);

                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            System.out.println("   📥 Parça " + blockIndex + " indirildi.");
                            fos.write(response.getBody());
                            blockIndex++;
                        } else {
                            moreParts = false; // توقف إذا لم نجد الجزء التالي
                        }
                    } catch (Exception e) {
                        // إذا أرجع السيرفر 404 أو أي خطأ، فهذا يعني انتهاء الأجزاء
                        moreParts = false;
                    }
                }
            }
            System.out.println("🎉 Dosya başarıyla birleştirildi: " + finalFile.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("❌ Hata: " + e.getMessage());
        }
    }

    // --- دالة الحذف (Delete) ---
    private void deleteFileRequest(String filename) {
        // البدء بعملية الحذف مع طباعة اسم الملف
        System.out.println("🗑️ Siliniyor: " + filename + "...");

        try {
            // 1. تشفير اسم الملف (URL Encoding) للتعامل مع المسافات والرموز الخاصة (مثل الأحرف التركية أو العربية)
            // هذا يمنع ظهور خطأ 400 Bad Request
            String encodedFileName = java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8.toString());

            // 2. بناء الرابط الموجه إلى الماستر (NameNode)
            String deleteUrl = MASTER_URL + "/api/file/delete/" + encodedFileName;

            // 3. استخدام exchange لاستقبال الرد النصي من الماستر
            // تم استخدام هذا الأسلوب بدلاً من restTemplate.delete لكي نتمكن من قراءة نص النجاح القادم من السيرفر
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl,
                    org.springframework.http.HttpMethod.DELETE,
                    null,
                    String.class
            );

            // 4. التحقق من نجاح العملية وطباعة الرد الفعلي القادم من الماستر
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ Sunucu Yanıtı: " + response.getBody());
            } else {
                System.out.println("⚠️ Silme başarısız. Durum kodu: " + response.getStatusCode());
            }

        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            // حالة عدم وجود الملف في سجلات الماستر (404)
            System.out.println("❌ Hata: Dosya bulunamadı (404).");
        } catch (Exception e) {
            // معالجة أي أخطاء أخرى مثل انقطاع الاتصال
            System.out.println("❌ Silme işlemi sırasında beklenmedik bir hata oluştu: " + e.getMessage());
        }
    }
// لازم نشغلها على ال cmd الحقيقي لان ماعم تشتغل على ال intellij
    private void clearScreen() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }
}