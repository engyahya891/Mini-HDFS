package com.hdfs.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

    private static String MASTER_IP = "localhost";
    private static String MASTER_URL = "http://" + MASTER_IP + ":8080";

    private final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);

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
        // 🟢 تمت إضافة ls للقائمة
        System.out.println("Kullanılabilir komutlar : ls, upload <file>, download <file>, delete <file>, clear, exit");

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

            // 🟢 1. أمر عرض الملفات (LS)
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
                System.out.println("Bilinmeyen komut. (Yardım: ls, upload, download, delete, clear, exit)");
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

    // 🟢 دالة عرض الملفات (جديدة)
    private void listFiles() {
        System.out.println("📂 Dosyalar listeleniyor...");
        try {
            // نستخدم ParameterizedTypeReference لاستقبال List<String>
            ResponseEntity<List<String>> response = restTemplate.exchange(
                    MASTER_URL + "/api/file/list",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<String>>() {}
            );

            List<String> files = response.getBody();
            if (files == null || files.isEmpty()) {
                System.out.println("📭 Sistemde hiç dosya yok (Empty).");
            } else {
                System.out.println("--------------------------------");
                System.out.println("📄 MEVCUT DOSYALAR (" + files.size() + "):");
                for (String f : files) {
                    System.out.println("   - " + f);
                }
                System.out.println("--------------------------------");
            }
        } catch (Exception e) {
            System.out.println("❌ Listeleme hatası: Master'a ulaşılamıyor veya endpoint yok.");
        }
    }

    // --- Upload ---
    private void uploadFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("❌ Dosya bulunamadı: " + path);
            return;
        }

        long fileSize = file.length();
        long blockSize = 64 * 1024 * 1024; // 64 MB
        int totalBlocks = (int) Math.ceil((double) fileSize / blockSize);

        System.out.println("📦 Dosya " + totalBlocks + " bloğa ayrılıyor...");

        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[(int) blockSize];
            int bytesRead;
            int blockIndex = 1;

            while ((bytesRead = fis.read(buffer)) != -1) {
                System.out.println("\n🔹 Blok numarası işleniyor: " + blockIndex + " / " + totalBlocks);

                com.hdfs.common.protocol.BlockAllocation request = new com.hdfs.common.protocol.BlockAllocation();
                request.setBlockIndex(blockIndex);

                String allocateUrl = MASTER_URL + "/api/file/allocate-block";
                com.hdfs.common.protocol.BlockAllocation response = restTemplate.postForObject(
                        allocateUrl, request, com.hdfs.common.protocol.BlockAllocation.class);

                if (response == null || response.getWorkerUrls() == null || response.getWorkerUrls().isEmpty()) {
                    System.out.println("❌ Başarısız: Master'dan uygun Worker adresi alınamadı.");
                    break;
                }

                byte[] exactData = java.util.Arrays.copyOf(buffer, bytesRead);

                for (String workerUrl : response.getWorkerUrls()) {
                    System.out.println("🚀 Kopya yükleniyor: " + workerUrl);
                    try {
                        final String partName = file.getName() + "_part_" + blockIndex;

                        org.springframework.core.io.ByteArrayResource byteResource = new org.springframework.core.io.ByteArrayResource(exactData) {
                            @Override
                            public String getFilename() {
                                return partName;
                            }
                        };

                        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

                        org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
                        body.add("file", byteResource);

                        org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                                new org.springframework.http.HttpEntity<>(body, headers);

                        restTemplate.postForObject(workerUrl + "/api/data/write", entity, String.class);
                        System.out.println("      ✅ " + partName + " başarıyla yüklendi.");

                    } catch (Exception e) {
                        System.out.println("      ⚠️ " + workerUrl + " hatası: " + e.getMessage());
                    }
                }
                blockIndex++;
            }
            System.out.println("\n🎉 Tüm bloklar başarıyla yüklendi!");

        } catch (Exception e) {
            System.out.println("❌ Yükleme hatası: " + e.getMessage());
        }
    }

    // --- Download ---
    private void downloadFile(String filename) {
        System.out.println("🔄 Master'a soruluyor...");
        String targetFolder = "C:\\HDFS_Downloads\\";

        // التأكد من وجود مجلد التنزيلات
        new File(targetFolder).mkdirs();

        try {
            // التشفير مهم للتعامل مع المسافات
            String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString());
            String locateUrl = MASTER_URL + "/api/file/locate/" + encodedName;

            String workerUrl = restTemplate.getForObject(locateUrl, String.class);

            if (workerUrl == null || workerUrl.equals("DOSYA_BULUNAMADI")) {
                System.out.println("⛔ Dosya Master kayıtlarında bulunamadı!");
                return;
            }

            File finalFile = new File(targetFolder + filename);
            try (FileOutputStream fos = new FileOutputStream(finalFile)) {
                int blockIndex = 1;
                boolean moreParts = true;

                while (moreParts) {
                    // بناء اسم الجزء (يجب أن نتأكد أن الاسم مشفر عند إرساله في الرابط)
                    String partName = filename + "_part_" + blockIndex;
                    String encodedPartName = URLEncoder.encode(partName, StandardCharsets.UTF_8.toString());

                    String downloadUrl = workerUrl + "/api/data/read/" + encodedPartName;

                    try {
                        org.springframework.http.ResponseEntity<byte[]> response =
                                restTemplate.getForEntity(downloadUrl, byte[].class);

                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            System.out.println("   📥 Parça " + blockIndex + " indirildi.");
                            fos.write(response.getBody());
                            blockIndex++;
                        } else {
                            moreParts = false;
                        }
                    } catch (Exception e) {
                        moreParts = false; // نهاية الملف
                    }
                }
            }
            System.out.println("🎉 Dosya indirildi: " + finalFile.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("❌ Hata: " + e.getMessage());
        }
    }

    // --- Delete ---
    private void deleteFileRequest(String filename) {
        System.out.println("🗑️ Siliniyor: " + filename + "...");

        try {
            // التشفير ضروري لأننا نستخدم URL
            String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString());

            String deleteUrl = MASTER_URL + "/api/file/delete/" + encodedFileName;

            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl,
                    org.springframework.http.HttpMethod.DELETE,
                    null,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ " + response.getBody());
            } else {
                System.out.println("⚠️ Hata Kodu: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.out.println("❌ Silme hatası: " + e.getMessage());
        }
    }

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