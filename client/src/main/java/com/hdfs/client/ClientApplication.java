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

    private static String CURRENT_USER = null;
    private static boolean isLoggedIn = false;

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

        System.out.print("✍️  Lütfen Master IP adresini girin (Default: localhost): ");
        String inputIp = scanner.nextLine().trim();
        if (!inputIp.isEmpty()) {
            MASTER_IP = inputIp;
            MASTER_URL = "http://" + MASTER_IP + ":8080";
        }

        System.out.println("✅ Master adresi: " + MASTER_URL);
        System.out.println("------------------------------------------------");
        System.out.println("Mini-HDFS İstemcisine Hoş Geldiniz!");

        while (true) {
            // 🟢 1. حالة الضيف
            if (!isLoggedIn) {
                System.out.println("\n--- Lütfen bir işlem seçin ---");
                System.out.println("1. Giriş Yap (تسجيل الدخول)");
                System.out.println("2. Yeni Kayıt (إنشاء حساب جديد)");
                System.out.println("3. Çıkış (خروج)");
                System.out.print("Seçiminiz (1/2/3): ");

                String choice = scanner.nextLine().trim();

                if ("3".equals(choice) || "exit".equalsIgnoreCase(choice)) {
                    System.out.println("👋 Güle güle! Çıkış yapılıyor...");
                    break;
                }
                else if ("1".equals(choice)) {
                    System.out.print("👤 Kullanıcı Adı (اسم المستخدم): ");
                    String uname = scanner.nextLine().trim();
                    System.out.print("🔑 Şifre (كلمة السر): ");
                    String pass = scanner.nextLine().trim();

                    if (uname.isEmpty() || pass.isEmpty()) {
                        System.out.println("⚠️ İsim veya şifre boş bırakılamaz!");
                        continue;
                    }

                    if (performLogin(uname, pass)) {
                        isLoggedIn = true;
                        CURRENT_USER = uname;
                        System.out.println("✅ Hoş geldin, " + CURRENT_USER + "!");
                        System.out.println("Dosya komutları açıldı: ls, upload <file>, download <file>, delete <file>, logout");
                    }
                }
                else if ("2".equals(choice)) {
                    System.out.print("👤 Yeni Kullanıcı Adı (اسم المستخدم الجديد): ");
                    String uname = scanner.nextLine().trim();

                    if (uname.isEmpty()) {
                        System.out.println("⚠️ İsim boş bırakılamaz! (لا يمكن ترك الاسم فارغاً)");
                        continue;
                    }

                    // 🟢 التحقق الفوري من توفر الاسم قبل طلب كلمة السر
                    if (isUsernameTaken(uname)) {
                        System.out.println("❌ Bu isim zaten mevcut, lütfen başka bir isim seçin. (هذا الاسم موجود بالفعل، الرجاء اختيار اسم آخر)");
                        continue; // العودة للقائمة فوراً وعدم إكمال الخطوات
                    }

                    System.out.print("🔑 Yeni Şifre (كلمة السر الجديدة): ");
                    String pass = scanner.nextLine().trim();

                    System.out.print("🔑 Şifreyi Onayla (تأكيد كلمة السر): ");
                    String confirmPass = scanner.nextLine().trim();

                    if (pass.isEmpty()) {
                        System.out.println("⚠️ Şifre boş bırakılamaz! (لا يمكن ترك كلمة السر فارغة)");
                        continue;
                    }

                    if (!pass.equals(confirmPass)) {
                        System.out.println("❌ Şifreler eşleşmiyor! Lütfen tekrar deneyin. (كلمتا المرور غير متطابقتين! يرجى المحاولة مرة أخرى)");
                        continue;
                    }

                    System.out.println("\n⚠️ DİKKAT: Lütfen şifrenizi unutmayın, çünkü sistemde şifre sıfırlama özelliği yoktur!");
                    System.out.println("⚠️ تنبيه: الرجاء الاحتفاظ بكلمة المرور جيداً لأنه لا يمكن استعادتها أو تغييرها لاحقاً!\n");

                    performRegister(uname, pass);
                }
                else {
                    System.out.println("❓ Geçersiz seçim. Lütfen 1, 2 veya 3 girin.");
                }
            }
            // 🟢 2. حالة المستخدم المسجل
            else {
                System.out.print(CURRENT_USER + "@hdfs> ");
                String commandLine = scanner.nextLine().trim();

                if (commandLine.isEmpty()) continue;

                String[] parts = commandLine.split("\\s+", 3);
                String command = parts[0];

                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("👋 Güle güle " + CURRENT_USER + "! Çıkış yapılıyor...");
                    break;
                } else if ("logout".equalsIgnoreCase(command)) {
                    System.out.println("👋 Çıkış yapıldı. Tekrar bekleriz " + CURRENT_USER + "!");
                    isLoggedIn = false;
                    CURRENT_USER = null;
                } else if ("clear".equalsIgnoreCase(command)) {
                    clearScreen();
                } else if ("ls".equalsIgnoreCase(command)) {
                    listFiles();
                } else if ("upload".equalsIgnoreCase(command)) {
                    if (parts.length < 2) {
                        System.out.println("⚠️ Lütfen dosya yolunu belirtin.");
                        continue;
                    }
                    String path = parts.length == 3 ? parts[1] + " " + parts[2] : parts[1];
                    uploadFile(removeQuotes(path));
                } else if ("download".equalsIgnoreCase(command)) {
                    if (parts.length < 2) {
                        System.out.println("⚠️ Lütfen dosya adını belirtin.");
                        continue;
                    }
                    String path = parts.length == 3 ? parts[1] + " " + parts[2] : parts[1];
                    downloadFile(removeQuotes(path));
                } else if ("delete".equalsIgnoreCase(command)) {
                    if (parts.length < 2) {
                        System.out.println("⚠️ Lütfen silinecek dosya adını belirtin.");
                        continue;
                    }
                    String path = parts.length == 3 ? parts[1] + " " + parts[2] : parts[1];
                    deleteFileRequest(removeQuotes(path));
                } else {
                    System.out.println("❓ Bilinmeyen komut. (Yardım: ls, upload, download, delete, logout, clear, exit)");
                }
            }
        }
    }

    // 🟢 --- دالة التحقق الفوري من اسم المستخدم ---
    private boolean isUsernameTaken(String username) {
        try {
            String url = MASTER_URL + "/api/auth/check-username?username=" + username;
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            // في حال فشل الاتصال، نفترض أنه متاح لكي تظهر رسالة الخطأ الحقيقية من السيرفر لاحقاً
            return false;
        }
    }

    // 🟢 --- دالة تسجيل الدخول (معدلة لتوضيح سبب الرفض) ---
    private boolean performLogin(String username, String password) {
        try {
            String url = MASTER_URL + "/api/auth/login?username=" + username + "&password=" + password;
            restTemplate.postForEntity(url, null, String.class);
            return true;
        } catch (HttpClientErrorException e) {
            // 🟢 التقاط حالة الخطأ بدقة وطباعة رسالة مخصصة
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.out.println("❌ Kullanıcı bulunamadı! Lütfen önce kayıt olun. (المستخدم غير موجود! الرجاء إنشاء حساب أولاً)");
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                System.out.println("❌ Yanlış şifre! Lütfen tekrar deneyin. (كلمة المرور خاطئة! يرجى المحاولة مرة أخرى)");
            } else {
                System.out.println("❌ Giriş başarısız. (فشل تسجيل الدخول)");
            }
            return false;
        } catch (Exception e) {
            System.out.println("❌ Hata: Master'a bağlanılamadı. (خطأ: لا يمكن الاتصال بالخادم)");
            return false;
        }
    }

    private void performRegister(String username, String password) {
        try {
            String url = MASTER_URL + "/api/auth/register?username=" + username + "&password=" + password;
            restTemplate.postForEntity(url, null, String.class);
            System.out.println("✅ Kayıt başarıyla tamamlandı. Hoş geldin, " + username + "!");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                System.out.println("❌ Bu isim zaten mevcut, lütfen başka bir isim seçin. (هذا الاسم موجود بالفعل، الرجاء اختيار اسم آخر)");
            } else {
                System.out.println("❌ " + e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            System.out.println("❌ Hata: Master'a bağlanılamadı.");
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
                    listUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {}
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

    // --- Upload ---
    private void uploadFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("❌ Dosya bulunamadı: " + path);
            return;
        }

        long fileSize = file.length();
        long blockSize = 64 * 1024 * 1024;
        int totalBlocks = (int) Math.ceil((double) fileSize / blockSize);

        System.out.println("📦 Dosya: " + file.getName() + " (" + totalBlocks + " blok) yükleniyor...");

        long startTime = System.currentTimeMillis();
        boolean uploadSuccess = true;

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
                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        System.out.println("\n❌ KRİTİK HATA: Yükleme sırasında dosya sunucudan silindi!");
                        System.out.println("🛑 Yükleme işlemi derhal iptal ediliyor.");
                        uploadSuccess = false;
                        break;
                    }
                }

                if (!uploadSuccess) break;

                if (response == null || response.getWorkerUrls() == null || response.getWorkerUrls().isEmpty()) {
                    System.out.println("❌ Başarısız: Master'dan uygun Worker adresi alınamadı.");
                    uploadSuccess = false;
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

            if (uploadSuccess) {
                System.out.println("\n🎉 Yükleme tamamlandı!");
            }

        } catch (Exception e) {
            System.out.println("❌ Yükleme hatası: " + e.getMessage());
            uploadSuccess = false;
        }

        if (uploadSuccess) {
            long endTime = System.currentTimeMillis();
            printPerformanceReport(fileSize, startTime, endTime, "Upload");
        } else {
            System.out.println("⚠️ İşlem yarıda kesildiği için performans raporu oluşturulmadı.");
        }
    }

    // --- Download ---
    private void downloadFile(String filename) {
        System.out.println("🔄 İndiriliyor: " + filename);
        String targetFolder = "C:\\HDFS_Downloads\\";
        new File(targetFolder).mkdirs();

        try {
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

                    String downloadUrl = workerUrl + "/api/data/read/" + encodedPartName;

                    try {
                        ResponseEntity<byte[]> response = restTemplate.getForEntity(downloadUrl, byte[].class);
                        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                            System.out.print(".");
                            fos.write(response.getBody());
                            downloadedBlocks++;
                            blockIndex++;
                        }
                    } catch (Exception e) {
                        System.out.println("\n❌ " + partName + " indirilirken " + workerUrl + " düğümünde hata oluştu!");
                        moreParts = false;
                    }
                }

                boolean fileStillExists = true;
                try {
                    restTemplate.getForEntity(locateUrl, String.class);
                } catch (Exception e) {
                    fileStillExists = false;
                }

                if (downloadedBlocks > 0 && fileStillExists) {
                    success = true;
                } else if (!fileStillExists) {
                    System.out.println("\n❌ KRİTİK HATA: İndirme işlemi sırasında dosya sunucudan silindi veya değiştirildi!");
                    success = false;
                }

            } catch (Exception e) {
                System.out.println("\n❌ Yazma hatası: " + e.getMessage());
            }

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
            String encodedFileName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String deleteUrl = MASTER_URL + "/api/file/delete/" + encodedFileName + "?owner=" + CURRENT_USER;

            ResponseEntity<String> response = restTemplate.exchange(
                    deleteUrl, HttpMethod.DELETE, null, String.class
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

    // --- Performance Report ---
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