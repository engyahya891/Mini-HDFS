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
                deleteFileRequest(parts[1]);

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

    // --- دالة الرفع (Upload) مع التقطيع والتكرار ---
    private void uploadFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("❌ Dosya bulunamadı!");
            return;
        }

        long fileSize = file.length();
        long blockSize = 64 * 1024 * 1024; // 64 MB

        int totalBlocks = (int) Math.ceil((double) fileSize / blockSize);
        System.out.println("📦 Dosya " + totalBlocks + "Bloklara ayrılıyor...");

        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[(int) blockSize];
            int bytesRead;
            int blockIndex = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                blockIndex++;
                System.out.println("\n🔹 Blok numarası işleniyor " + blockIndex + " / " + totalBlocks);

                // 🟢 استخدام MASTER_URL المتغير
                com.hdfs.common.protocol.BlockAllocation request = new com.hdfs.common.protocol.BlockAllocation();
                request.setBlockIndex(blockIndex);

                String allocateUrl = MASTER_URL + "/api/file/allocate-block";
                com.hdfs.common.protocol.BlockAllocation response = restTemplate.postForObject(
                        allocateUrl, request, com.hdfs.common.protocol.BlockAllocation.class);

                if (response == null || response.getWorkerUrls() == null || response.getWorkerUrls().isEmpty()) {
                    System.out.println("❌ Başarısız: Master, sunuculardan yanıt alamadı.");
                    break;
                }

                // حلقة التكرار (Replication)
                for (String workerUrl : response.getWorkerUrls()) {
                    System.out.println("🚀 kopya yükleniyor: " + workerUrl);
                    try {
                        byte[] exactData = java.util.Arrays.copyOf(buffer, bytesRead);

                        org.springframework.core.io.ByteArrayResource byteResource = new org.springframework.core.io.ByteArrayResource(exactData) {
                            @Override
                            public String getFilename() {
                                return file.getName() + "_part_" + response.getBlockIndex();
                            }
                        };

                        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

                        org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
                        body.add("file", byteResource);

                        org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                                new org.springframework.http.HttpEntity<>(body, headers);

                        restTemplate.postForObject(workerUrl + "/api/data/write", entity, String.class);
                        System.out.println("      ✅ Başarılı (Uploaded).");

                    } catch (Exception e) {
                        System.out.println("      ⚠️ Başarısız: " + e.getMessage());
                    }
                }
            }
            System.out.println("\n🎉 Yükleme ve yedekleme işlemi başarıyla tamamlandı!");

        } catch (Exception e) {
            System.out.println("❌ Yükleme sırasında hata oluştu: " + e.getMessage());
        }
    }

    // --- دالة التحميل (Download) مع التجميع (Reassembly) ---
    private void downloadFile(String filename) {
        System.out.println("🔄 Master'a soruluyor... (Asking Master...)");
        String targetFolder = "C:\\HDFS_Downloads\\";

        try {
            File directory = new File(targetFolder);
            if (!directory.exists()) directory.mkdirs();

            // 🟢 1. استخدام MASTER_URL
            String locateUrl = MASTER_URL + "/api/file/locate/" + filename;
            String workerUrl = restTemplate.getForObject(locateUrl, String.class);

            if ("DOSYA_BULUNAMADI".equals(workerUrl) || workerUrl == null) {
                System.out.println("⛔ Dosya Master kayıtlarında yok! (Dosya bulunamadı)");
                return;
            }

            System.out.println("📍 Kaynak Worker: " + workerUrl);
            System.out.println("⬇️ İndirme ve Birleştirme Başlıyor...");

            File finalFile = new File(targetFolder + filename);

            // 🟢 2. فتح دفق للكتابة وتجميع الأجزاء
            try (FileOutputStream fos = new FileOutputStream(finalFile)) {
                int blockIndex = 1;
                while (true) {
                    try {
                        String partName = filename + "_part_" + blockIndex;
                        String downloadUrl = workerUrl + "/api/data/read/" + partName;

                        System.out.print("   📥 Parça " + blockIndex + " indiriliyor... ");
                        byte[] fileBytes = restTemplate.getForObject(downloadUrl, byte[].class);

                        if (fileBytes == null || fileBytes.length == 0) break;

                        fos.write(fileBytes); // دمج البيانات
                        System.out.println("✅ Tamam.");
                        blockIndex++;

                    } catch (Exception e) {
                        System.out.println("\n🏁 İndirme tamamlandı (Son parça).");
                        break;
                    }
                }
            }
            System.out.println("🎉 Dosya başarıyla kaydedildi: " + finalFile.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("❌ Hata: " + e.getMessage());
        }
    }

    // --- دالة الحذف (Delete) ---
    private void deleteFileRequest(String filename) {
        System.out.println("🗑️ Siliniyor " + filename + "...");
        try {
            // 🟢 استخدام MASTER_URL
            String deleteUrl = MASTER_URL + "/api/file/delete/" + filename;
            restTemplate.delete(deleteUrl);
            System.out.println("✅ Dosya başarıyla silindi. (Tüm Node'lerden)!");

        } catch (Exception e) {
            System.out.println("❌ Silme başarısız: " + e.getMessage());
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