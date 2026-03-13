package com.hdfs.client;

import com.hdfs.common.protocol.BlockAllocation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

    private static String MASTER_IP = "localhost";
    private static String MASTER_URL = "http://" + MASTER_IP + ":8080";
    private static String CURRENT_USER = "anonymous";

    private final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("========================================");
        System.out.println("⚙️  HDFS Client Configuration");
        System.out.println("========================================");

        // 1. إعداد الـ IP
        System.out.print("✍️  Lütfen Master IP adresini girin (Default: localhost): ");
        String inputIp = scanner.nextLine().trim();
        if (!inputIp.isEmpty()) {
            MASTER_IP = inputIp;
            MASTER_URL = "http://" + MASTER_IP + ":8080";
        }

        // 2. تسجيل الدخول
        System.out.print("👤 Kullanıcı Adı Girin (Login): ");
        String inputUser = scanner.nextLine().trim();
        if (!inputUser.isEmpty()) {
            CURRENT_USER = inputUser;
        }

        System.out.println("✅ Giriş yapıldı: " + CURRENT_USER);
        System.out.println("✅ Master adresi: " + MASTER_URL);
        System.out.println("------------------------------------------------");
        System.out.println("Mini-HDFS İstemcisine Hoş Geldiniz, " + CURRENT_USER + "!");
        System.out.println("Kullanılabilir komutlar : ls, upload <file>, download <file>, delete <file>, clear, exit");

        while (true) {
            System.out.print(CURRENT_USER + "@hdfs> ");
            String commandLine = scanner.nextLine().trim();

            if (commandLine.isEmpty()) continue;

            String[] parts = commandLine.split("\\s+", 2);
            String command = parts[0];

            if ("exit".equalsIgnoreCase(command)) {
                System.out.println("👋 Güle güle " + CURRENT_USER + "! Çıkış yapılıyor...");
                break;
            }

            if ("ls".equalsIgnoreCase(command)) {
                listFiles();

            } else if ("upload".equalsIgnoreCase(command)) {
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
                System.out.println("❓ Bilinmeyen komut. (Yardım: ls, upload, download, delete, clear, exit)");
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

    // --- List Files (LS) ---
    private void listFiles() {
        System.out.println("📂 Dosyalar listeleniyor (" + CURRENT_USER + ")...");
        try {
            String listUrl = MASTER_URL + "/api/file/list/" + CURRENT_USER;

            ResponseEntity<List<String>> response = restTemplate.exchange(
                    listUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            List<String> files = response.getBody();
            if (files == null || files.isEmpty()) {
                System.out.println("📭 Klasörünüz boş.");
            } else {
                System.out.println("--------------------------------");
                System.out.println("📄 DOSYALARINIZ (" + files.size() + "):");
                for (String f : files) {
                    System.out.println("   - " + f);
                }
                System.out.println("--------------------------------");
            }
        } catch (Exception e) {
            System.out.println("❌ Listeleme hatası: " + e.getMessage());
        }
    }

    // 🟢 --- Upload (مزودة بقياس الأداء) ---
    private void uploadFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("❌ Dosya bulunamadı: " + path);
            return;
        }

        long fileSize = file.length();
        long blockSize = 64 * 1024 * 1024; // 64 MB
        int totalBlocks = (int) Math.ceil((double) fileSize / blockSize);

        System.out.println("📦 Dosya: " + file.getName() + " (" + totalBlocks + " blok) yükleniyor...");

        // ⏱️ بدء المؤقت
        long startTime = System.currentTimeMillis();
        boolean uploadSuccess = false;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) blockSize];
            int bytesRead;
            int blockIndex = 1;

            while ((bytesRead = fis.read(buffer)) != -1) {
                System.out.println("\n🔹 Blok " + blockIndex + " ayrılıyor...");

                BlockAllocation request = new BlockAllocation();
                request.setBlockIndex(blockIndex);

                String allocateUrl = MASTER_URL + "/api/file/allocate-block?owner=" + CURRENT_USER
                        + "&filename=" + file.getName();

                BlockAllocation response = null;
                try {
                    response = restTemplate.postForObject(allocateUrl, request, BlockAllocation.class);
                } catch (HttpClientErrorException e) {
                    // 🟢 إذا الماستر أرسل خطأ 409 (Conflict)، فهذا يعني أن الملف حُذف!
                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        System.out.println("\n❌ KRİTİK HATA: Yükleme sırasında dosya sunucudan silindi!");
                        System.out.println("🛑 Yükleme işlemi derhal iptal ediliyor.");
                        uploadSuccess = false;
                        break; // نكسر حلقة الرفع فوراً!
                    }
                }

                if (response == null || response.getWorkerUrls() == null || response.getWorkerUrls().isEmpty()) {
                    System.out.println("❌ Başarısız: Master'dan uygun Worker adresi alınamadı.");
                    break;
                }

                byte[] exactData = Arrays.copyOf(buffer, bytesRead);

                for (String workerUrl : response.getWorkerUrls()) {
                    System.out.print("   🚀 Yükleniyor -> " + workerUrl + " ... ");
                    try {
                        final String partName = file.getName() + "_part_" + blockIndex;

                        ByteArrayResource byteResource = new ByteArrayResource(exactData) {
                            @Override
                            public String getFilename() { return partName; }
                        };

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                        body.add("file", byteResource);

                        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

                        restTemplate.postForObject(workerUrl + "/api/data/write", entity, String.class);
                        System.out.println("✅ Tamam.");

                    } catch (Exception e) {
                        System.out.println("⚠️ Hata!");
                    }
                }
                blockIndex++;
            }
            uploadSuccess = true;
            System.out.println("\n🎉 Yükleme tamamlandı!");

        } catch (Exception e) {
            System.out.println("❌ Yükleme hatası: " + e.getMessage());
        }

        // ⏱️ إيقاف المؤقت وحساب الأداء
        if (uploadSuccess) {
            long endTime = System.currentTimeMillis();
            printPerformanceReport(fileSize, startTime, endTime, "Upload");
        }
    }

    // 🟢 --- Download (المعدلة والمحمية ضد المسح أثناء التحميل) ---
    private void downloadFile(String filename) {
        System.out.println("🔄 İndiriliyor: " + filename);
        String targetFolder = "C:\\HDFS_Downloads\\";
        new File(targetFolder).mkdirs();

        try {
            // 1. التحقق المبدئي
            String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String locateUrl = MASTER_URL + "/api/file/locate/" + encodedName + "?owner=" + CURRENT_USER;

            try {
                restTemplate.getForEntity(locateUrl, String.class);
            } catch (HttpClientErrorException e) {
                System.out.println("❌ HATA: " + e.getResponseBodyAsString());
                return;
            }

            File finalFile = new File(targetFolder + filename);
            boolean success = false;
            int downloadedBlocks = 0;
            long startTime = System.currentTimeMillis();

            try (FileOutputStream fos = new FileOutputStream(finalFile)) {
                int blockIndex = 1;
                boolean moreParts = true;

                while (moreParts) {
                    String partName = filename + "_part_" + blockIndex;
                    String encodedPartName = URLEncoder.encode(partName, StandardCharsets.UTF_8.toString()).replace("+", "%20");

                    // نسأل الماستر عن مكان هذا الجزء
                    String locateBlockUrl = MASTER_URL + "/api/file/locate-block/" + encodedPartName;
                    String workerUrl = null;

                    try {
                        ResponseEntity<String> masterResp = restTemplate.getForEntity(locateBlockUrl, String.class);
                        workerUrl = masterResp.getBody();
                    } catch (HttpClientErrorException e) {
                        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                            moreParts = false;
                            continue;
                        }
                    }

                    if (workerUrl == null) {
                        moreParts = false;
                        continue;
                    }

                    // التحميل من الووركر
                    String downloadUrl = workerUrl + "/api/data/read/" + encodedPartName;

                    try {
                        ResponseEntity<byte[]> response = restTemplate.getForEntity(downloadUrl, byte[].class);
                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            System.out.print("."); // مؤشر تقدم
                            fos.write(response.getBody());
                            downloadedBlocks++;
                            blockIndex++;
                        }
                    } catch (Exception e) {
                        System.out.println("\n❌ " + partName + " indirilirken " + workerUrl + " düğümünde hata oluştu!");
                        moreParts = false;
                    }
                } // 🔚 نهاية حلقة while

                // 🟢 التحقق النهائي لضمان عدم تعرض الملف للحذف أثناء عملية التحميل
                boolean fileStillExists = true;
                try {
                    restTemplate.getForEntity(locateUrl, String.class);
                } catch (Exception e) {
                    // استخدام Exception للقبض على أي نوع من الأخطاء من الماستر
                    fileStillExists = false;
                }

                // 🟢 التقييم النهائي
                if (downloadedBlocks > 0 && fileStillExists) {
                    success = true;
                } else if (!fileStillExists) {
                    System.out.println("\n❌ KRİTİK HATA: İndirme işlemi sırasında dosya sunucudan silindi veya değiştirildi!");
                    success = false; // نلغي النجاح لكي يتم مسح الملف التالف
                }

            } catch (Exception e) {
                System.out.println("\n❌ Yazma hatası: " + e.getMessage());
            }

            // التنظيف في حال الفشل
            if (finalFile.exists() && (!success || finalFile.length() == 0)) {
                finalFile.delete();
                if (!success && downloadedBlocks > 0) {
                    System.out.println("⚠️ İndirme iptal edildi, eksik/bozuk dosya temizlendi.");
                }
            } else if (success) {
                System.out.println("\n🎉 Dosya başarıyla indirildi: " + finalFile.getAbsolutePath());
                long endTime = System.currentTimeMillis();
                printPerformanceReport(finalFile.length(), startTime, endTime, "Download");
            }

        } catch (Exception e) {
            System.out.println("❌ Genel Hata: " + e.getMessage());
        }
    }

    // --- Delete ---
    private void deleteFileRequest(String filename) {
        System.out.println("🗑️ Siliniyor: " + filename + "...");
        try {
            // 1. التشفير الصحيح للمسافات
            String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replace("+", "%20");

            // 2. بناء الرابط
            String deleteUrl = MASTER_URL + "/api/file/delete/" + encodedFileName + "?owner=" + CURRENT_USER;

            ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    null,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ " + response.getBody());
            } else {
                System.out.println("⚠️ " + response.getBody());
            }

        } catch (HttpClientErrorException e) {
            System.out.println("❌ İşlem Başarısız: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.out.println("❌ Hata: " + e.getMessage());
        }
    }

    private void clearScreen() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else { System.out.print("\033[H\033[2J"); System.out.flush(); }
        } catch (Exception e) { for (int i = 0; i < 50; i++) System.out.println(); }
    }

    // 🟢 دالة جديدة مخصصة لطباعة تقرير الأداء
    private void printPerformanceReport(long fileSizeBytes, long startTimeMs, long endTimeMs, String operationType) {
        long durationMs = endTimeMs - startTimeMs;
        if (durationMs == 0) durationMs = 1;

        double durationSeconds = durationMs / 1000.0;
        double fileSizeMB = fileSizeBytes / (1024.0 * 1024.0);
        double throughputMBps = fileSizeMB / durationSeconds;

        System.out.println("\n📊 --- PERFORMANS RAPORU (" + operationType + ") ---");
        System.out.printf("   📏 Dosya Boyutu : %.2f MB\n", fileSizeMB);
        System.out.printf("   ⏱️ Gecikme      : %.3f saniye\n", durationSeconds);
        System.out.printf("   🚀 Aktarım Hızı : %.2f MB/s\n", throughputMBps);
        System.out.println("-------------------------------------------");
    }
}