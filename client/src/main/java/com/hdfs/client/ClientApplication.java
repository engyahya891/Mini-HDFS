package com.hdfs.client;

import com.hdfs.common.protocol.BlockAllocation; // تأكد أن هذا الكلاس موجود
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
    private static String CURRENT_USER = "anonymous"; // 🟢 المتغير الجديد لاسم المستخدم

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

        // 🟢 2. تسجيل الدخول (طلب اسم المستخدم)
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
            // تغيير شكل المؤشر ليظهر اسم المستخدم
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
                listFiles(); // 🟢 تم تعديلها لترسل اسم المستخدم

            } else if ("upload".equalsIgnoreCase(command)) {
                if (parts.length < 2) {
                    System.out.println("⚠️ Lütfen dosya yolunu belirtin.");
                    continue;
                }
                uploadFile(removeQuotes(parts[1])); // 🟢 تم تعديلها لترسل اسم المستخدم

            } else if ("download".equalsIgnoreCase(command)) {
                // التنزيل لا يحتاج لتغيير (لأننا نبحث عن الملف بالاسم)
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

    // 🟢 تعديل دالة LS لتطلب ملفات المستخدم الحالي فقط
    private void listFiles() {
        System.out.println("📂 Dosyalar listeleniyor (" + CURRENT_USER + ")...");
        try {
            // الرابط الجديد: /api/file/list/{username}
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

    // 🟢 تعديل Upload لإرسال اسم المالك
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

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) blockSize];
            int bytesRead;
            int blockIndex = 1;

            while ((bytesRead = fis.read(buffer)) != -1) {
                System.out.println("\n🔹 Blok " + blockIndex + " ayrılıyor...");

                // إعداد طلب التخصيص
                BlockAllocation request = new BlockAllocation();
                request.setBlockIndex(blockIndex);


                // 🟢 التعديل: إرسال المالك + اسم الملف في الرابط
                String allocateUrl = MASTER_URL + "/api/file/allocate-block?owner=" + CURRENT_USER
                        + "&filename=" + file.getName();

                BlockAllocation response = restTemplate.postForObject(
                        allocateUrl, request, BlockAllocation.class);

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
            System.out.println("\n🎉 Yükleme tamamlandı!");

        } catch (Exception e) {
            System.out.println("❌ Yükleme hatası: " + e.getMessage());
        }
    }

    // --- Download (لم يتغير كثيراً) ---
    private void downloadFile(String filename) {
        System.out.println("🔄 İndiriliyor: " + filename);
        String targetFolder = "C:\\HDFS_Downloads\\";
        new File(targetFolder).mkdirs();

        try {
            String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString());
            String locateUrl = MASTER_URL + "/api/file/locate/" + encodedName;

            String workerUrl = restTemplate.getForObject(locateUrl, String.class);

            if (workerUrl == null || workerUrl.equals("DOSYA_BULUNAMADI")) {
                System.out.println("⛔ Dosya bulunamadı veya size ait değil!");
                return;
            }

            File finalFile = new File(targetFolder + filename);
            try (FileOutputStream fos = new FileOutputStream(finalFile)) {
                int blockIndex = 1;
                boolean moreParts = true;

                while (moreParts) {
                    String partName = filename + "_part_" + blockIndex;
                    String encodedPartName = URLEncoder.encode(partName, StandardCharsets.UTF_8.toString());
                    String downloadUrl = workerUrl + "/api/data/read/" + encodedPartName;

                    try {
                        ResponseEntity<byte[]> response = restTemplate.getForEntity(downloadUrl, byte[].class);
                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            System.out.print("."); // مؤشر تقدم بسيط
                            fos.write(response.getBody());
                            blockIndex++;
                        } else {
                            moreParts = false;
                        }
                    } catch (Exception e) {
                        moreParts = false;
                    }
                }
            }
            System.out.println("\n🎉 Dosya indirildi: " + finalFile.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("❌ Hata: " + e.getMessage());
        }
    }

    // --- Delete (لم يتغير) ---
    private void deleteFileRequest(String filename) {
        System.out.println("🗑️ Siliniyor: " + filename + "...");
        try {
            String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString());
            String deleteUrl = MASTER_URL + "/api/file/delete/" + encodedFileName;

            ResponseEntity<String> response = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, null, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ " + response.getBody());
            } else {
                System.out.println("⚠️ Hata: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.out.println("❌ Silme hatası: " + e.getMessage());
        }
    }

    private void clearScreen() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else { System.out.print("\033[H\033[2J"); System.out.flush(); }
        } catch (Exception e) { for (int i = 0; i < 50; i++) System.out.println(); }
    }
}