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
import java.security.MessageDigest; // 🟢 تمت الإضافة لـ MD5
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Scanner;

@SpringBootApplication
public class ClientApplication implements CommandLineRunner {

    private static String MASTER_IP = "localhost";
    private static String MASTER_URL = "http://" + MASTER_IP + ":8080";

    private static String CURRENT_USER = null;
    private static boolean isLoggedIn = false;
    private static boolean isAdmin = false;

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

                    if ("admin".equals(uname) && "admin1234".equals(pass)) {
                        isLoggedIn = true;
                        isAdmin = true;
                        CURRENT_USER = "admin";
                        System.out.println("\n✅ YÖNETİCİ GİRİŞİ BAŞARILI! (تم تسجيل دخول المدير)");
                        System.out.println("Ana Düğüm Yönetici Konsolu aktif edildi.");
                        System.out.println("Komutlar: status, list-workers, delete-worker <url>, active-w ,logout, clear");
                    }
                    else if (performLogin(uname, pass)) {
                        isLoggedIn = true;
                        isAdmin = false;
                        CURRENT_USER = uname;
                        System.out.println("✅ Hoş geldin, " + CURRENT_USER + "!");
                        System.out.println("Dosya komutları açıldı: ls, info <file>, upload <file>, download <file>, delete <file>, logout");
                    }
                }
                else if ("2".equals(choice)) {
                    System.out.print("👤 Yeni Kullanıcı Adı (اسم المستخدم Yeni): ");
                    String uname = scanner.nextLine().trim();

                    if (uname.isEmpty()) {
                        System.out.println("⚠️ İsim boş bırakılamaz! (لا يمكن ترك الاسم فارغاً)");
                        continue;
                    }

                    if (isUsernameTaken(uname)) {
                        System.out.println("❌ Bu isim zaten mevcut, lütfen başka bir isim seçin. (هذا الاسم موجود بالفعل، الرجاء اختيار اسم آخر)");
                        continue;
                    }

                    System.out.print("🔑 Yeni Şifre (كلمة السر Yeni): ");
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
                    performRegister(uname, pass);
                }
                else {
                    System.out.println("❓ Geçersiz seçim. Lütfen 1, 2 veya 3 girin.");
                }
            }
            // 🟢 2. حالة المدير
            else if (isAdmin) {
                System.out.print("⚙️ " + CURRENT_USER + "@hdfs> ");
                String commandLine = scanner.nextLine().trim();

                if (commandLine.isEmpty()) continue;

                String[] parts = commandLine.split("\\s+", 2);
                String command = parts[0];

                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("👋 Sistem kapatılıyor...");
                    break;
                } else if ("logout".equalsIgnoreCase(command)) {
                    System.out.println("👋 Yönetici oturumu kapatıldı.");
                    isLoggedIn = false;
                    isAdmin = false;
                    CURRENT_USER = null;
                } else if ("clear".equalsIgnoreCase(command)) {
                    clearScreen();
                } else if ("status".equalsIgnoreCase(command)) {
                    printClusterStatus();
                } else if ("list-workers".equalsIgnoreCase(command)) {
                    listWorkers(false);
                } else if ("active-w".equalsIgnoreCase(command)) {
                    listWorkers(true);
                } else if ("delete-worker".equalsIgnoreCase(command)) {
                    if (parts.length < 2) {
                        System.out.println("⚠️ Kullanım: delete-worker <url>");
                    } else {
                        deleteWorker(parts[1]);
                    }
                } else {
                    System.out.println("❓ Bilinmeyen yönetici komutu. (Yardım: status, list-workers, delete-worker <url>, logout, clear)");
                }
            }
            // 🟢 3. حالة المستخدم العادي
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
                } else if ("info".equalsIgnoreCase(command)) { // 🟢 تمت الإضافة: أمر info
                    if (parts.length < 2) {
                        System.out.println("⚠️ Lütfen dosya adını belirtin.");
                        continue;
                    }
                    String path = parts.length == 3 ? parts[1] + " " + parts[2] : parts[1];
                    printFileInfo(removeQuotes(path));
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
                    System.out.println("❓ Bilinmeyen komut. (Yardım: ls, info, upload, download, delete, logout, clear, exit)");
                }
            }
        }
    }

    // ==========================================================
    // 🟢 دوال المدير
    // ==========================================================
    private void printClusterStatus() {
        try {
            String url = MASTER_URL + "/api/admin/status";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> status = response.getBody();
            System.out.println("\n📊 --- CLUSTER DURUMU (حالة النظام) ---");
            System.out.println("🟢 Aktif Worker Sayısı : " + status.get("activeWorkers") + " düğüm (Nodes)");
            System.out.println("💾 Toplam Dosya Sayısı : " + status.get("totalFiles") + " dosya");
            System.out.println("👤 Kayıtlı Kullanıcılar: " + status.get("totalUsers") + " kullanıcı");
            System.out.println("⚡ Sistem Durumu       : " + status.get("health"));
            System.out.println("----------------------------------------\n");
        } catch (Exception e) {
            System.out.println("❌ Hata: Master'dan durum bilgisi alınamadı.");
        }
    }

    private void listWorkers(boolean onlyActive) {
        try {
            String url = MASTER_URL + "/api/admin/workers";
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> workers = response.getBody();

            if (workers == null || workers.isEmpty()) {
                System.out.println("📋 Sistemde kayıtlı Worker bulunmuyor!");
                return;
            }

            if (onlyActive) {
                workers = workers.stream()
                        .filter(w -> (Boolean) w.get("active"))
                        .collect(java.util.stream.Collectors.toList());
                System.out.println("\n🖥️ --- AKTİF ÇALIŞAN DÜĞÜMLER (" + workers.size() + ") ---");
            } else {
                System.out.println("\n📋 Kayıtlı Çalışan Düğümler (" + workers.size() + "):");
            }

            if (workers.isEmpty()) {
                System.out.println("   Aktif Worker bulunmuyor!");
                return;
            }

            for (Map<String, Object> w : workers) {
                String workerUrl = (String) w.get("url");
                boolean isActive = (Boolean) w.get("active");
                long secondsAgo = ((Number) w.get("secondsAgo")).longValue();

                String status = isActive ? "🟢 ÇEVRİMİÇİ" : "🔴 ÇEVRİMDIŞI";

                System.out.println("\n   🌍 URL      : " + workerUrl);
                System.out.println("      Durum    : " + status + " (Son aktivite: " + secondsAgo + " sn önce)");

                String storageInfo = (String) w.get("storageInfo");
                if (storageInfo != null) {
                    System.out.println("      Depolama : " + storageInfo);
                } else {
                    long used = ((Number) w.get("used")).longValue();
                    long capacity = ((Number) w.get("capacity")).longValue();
                    long usedMB = used / (1024 * 1024);
                    long capMB  = capacity / (1024 * 1024);
                    System.out.println("      Depolama : " + usedMB + " MB / " + capMB + " MB");
                }
            }
            System.out.println("\n--------------------------------");

        } catch (Exception e) {
            System.out.println("❌ Hata: Worker listesi alınamadı. (" + e.getMessage() + ")");
        }
    }

    private void deleteWorker(String workerUrl) {
        try {
            String url = MASTER_URL + "/api/admin/workers/delete?url=" + workerUrl;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, null, String.class);
            System.out.println(response.getBody());
        } catch (Exception e) {
            System.out.println("❌ İşlem Başarısız: Master'a ulaşılamadı veya Worker bulunamadı.");
        }
    }

    // ==========================================================
    // 🟢 دوال المستخدم
    // ==========================================================
    private boolean isUsernameTaken(String username) {
        try {
            String url = MASTER_URL + "/api/auth/check-username?username=" + username;
            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) { return false; }
    }

    private boolean performLogin(String username, String password) {
        try {
            String url = MASTER_URL + "/api/auth/login?username=" + username + "&password=" + password;
            restTemplate.postForEntity(url, null, String.class);
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) System.out.println("❌ Kullanıcı bulunamadı! Lütfen önce kayıt olun.");
            else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) System.out.println("❌ Yanlış şifre! Lütfen tekrar deneyin.");
            else System.out.println("❌ Giriş başarısız.");
            return false;
        } catch (Exception e) {
            System.out.println("❌ Hata: Master'a bağlanılamadı.");
            return false;
        }
    }

    private void performRegister(String username, String password) {
        try {
            String url = MASTER_URL + "/api/auth/register?username=" + username + "&password=" + password;
            restTemplate.postForEntity(url, null, String.class);
            System.out.println("✅ Kayıt başarıyla tamamlandı. Hoş geldin, " + username + "!");
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) System.out.println("❌ Bu isim zaten mevcut, lütfen başka bir isim seçin.");
        } catch (Exception e) { System.out.println("❌ Hata: Master'a bağlanılamadı."); }
    }

    private String removeQuotes(String path) {
        path = path.trim();
        if (path.startsWith("\"") && path.endsWith("\"")) return path.substring(1, path.length() - 1);
        return path;
    }

    // 🟢 تمت الإضافة: دالة عرض تفاصيل الملف (File Info)
    private void printFileInfo(String filename) {
        try {
            String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replace("+", "%20");
            String infoUrl = MASTER_URL + "/api/file/info/" + encodedName + "?owner=" + CURRENT_USER;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    infoUrl, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> info = response.getBody();
            if (info == null) {
                System.out.println("❌ Dosya bulunamadı.");
                return;
            }

            System.out.println("\n📄 --- DOSYA BİLGİSİ (File Info) ---");
            System.out.println("   Dosya Adı    : " + info.get("filename"));
            long size = ((Number) info.get("size")).longValue();
            System.out.printf("   Boyut        : %.2f MB\n", (size / (1024.0 * 1024.0)));
            System.out.println("   Blok Sayısı  : " + info.get("blocksCount") + " Blok");
            System.out.println("   🔒 Checksum  : " + info.get("checksum"));

            Map<String, List<String>> locations = (Map<String, List<String>>) info.get("locations");
            System.out.println("   🌍 Blokların Konumu (Replikasyon):");
            if (locations == null || locations.isEmpty()) {
                System.out.println("      - Konum bilgisi yok veya bloklar kayıp!");
            } else {
                int bIndex = 1;
                for (Map.Entry<String, List<String>> entry : locations.entrySet()) {
                    System.out.print("      - Blok " + bIndex + ": ");
                    System.out.println(String.join(", ", entry.getValue()));
                    bIndex++;
                }
            }
            System.out.println("------------------------------------");

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) System.out.println("❌ HATA: Dosya bulunamadı veya size ait değil.");
            else System.out.println("❌ HATA: Bilgi alınamadı.");
        } catch (Exception e) {
            System.out.println("❌ Sistem Hatası: " + e.getMessage());
        }
    }

    private void listFiles() {
        System.out.println("📂 Dosyalar listeleniyor ... ");
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
                for (String f : files) System.out.println("   - " + f);
                System.out.println("--------------------------------");
            }
        } catch (Exception e) { System.out.println("❌ Listeleme hatası: " + e.getMessage()); }
    }

    // 🟢 الرفع + حساب MD5 وإرساله
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
        MessageDigest md5Digest; // 🟢 حساب البصمة
        try {
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            System.out.println("❌ MD5 başlatılamadı.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[(int) blockSize];
            int bytesRead;
            int blockIndex = 1;

            while ((bytesRead = fis.read(buffer)) != -1) {
                // 🟢 تحديث البصمة مع كل بلوك نقرأه
                md5Digest.update(buffer, 0, bytesRead);

                BlockAllocation request = new BlockAllocation();
                request.setBlockIndex(blockIndex);

                String allocateUrl = MASTER_URL + "/api/file/allocate-block?owner=" + CURRENT_USER
                        + "&filename=" + file.getName();

                BlockAllocation response = null;
                try {
                    response = restTemplate.postForObject(allocateUrl, request, BlockAllocation.class);
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        System.out.println("\n\n❌ KRİTİK HATA: Yükleme sırasında dosya sunucudan silindi!");
                        System.out.println("🛑 Yükleme işlemi derhal iptal ediliyor.");
                        uploadSuccess = false;
                        break;
                    }
                }

                if (!uploadSuccess) break;

                if (response == null || response.getWorkerUrls() == null || response.getWorkerUrls().isEmpty()) {
                    System.out.println("\n❌ Başarısız: Master'dan uygun Worker adresi alınamadı.");
                    uploadSuccess = false;
                    break;
                }

                byte[] exactData = Arrays.copyOf(buffer, bytesRead);

                for (String workerUrl : response.getWorkerUrls()) {
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

                    } catch (Exception e) { }
                }

                printProgressBar("📤 Yükleniyor:", blockIndex, totalBlocks);
                blockIndex++;
            }

            if (uploadSuccess) {
                System.out.println("\n🎉 Yükleme tamamlandı!");

                // 🟢 بعد نجاح الرفع، نرسل البصمة للماستر
                String checksum = bytesToHex(md5Digest.digest());
                try {
                    String updateUrl = MASTER_URL + "/api/file/update-checksum?filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.toString()) + "&owner=" + CURRENT_USER + "&fileSize=" + fileSize + "&checksum=" + checksum;
                    restTemplate.postForEntity(updateUrl, null, String.class);
                    System.out.println("🔒 MD5 Bütünlük Özeti Kaydedildi: " + checksum);
                } catch (Exception e) {
                    System.out.println("⚠️ Checksum Master'a kaydedilemedi.");
                }
            }

        } catch (Exception e) {
            System.out.println("\n❌ Yükleme hatası: " + e.getMessage());
            uploadSuccess = false;
        }

        if (uploadSuccess) {
            long endTime = System.currentTimeMillis();
            printPerformanceReport(fileSize, startTime, endTime, "Upload");
        } else {
            System.out.println("⚠️ İşlem yarıda kesildiği için performans raporu oluşturulmadı.");
        }
    }

    // 🟢 التحميل + التأكد من بصمة MD5
    private void downloadFile(String filename) {
        System.out.println("🔄 İndiriliyor: " + filename);
        String targetFolder = "C:\\HDFS_Downloads\\";
        new File(targetFolder).mkdirs();

        try {
            String encodedName = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString()).replace("+", "%20");

            // 🟢 إحضار البصمة المتوقعة من الماستر أولاً
            String expectedChecksum = "YOK";
            try {
                String infoUrl = MASTER_URL + "/api/file/info/" + encodedName + "?owner=" + CURRENT_USER;
                ResponseEntity<Map<String, Object>> infoResp = restTemplate.exchange(infoUrl, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {});
                if(infoResp.getBody() != null && infoResp.getBody().get("checksum") != null) {
                    expectedChecksum = (String) infoResp.getBody().get("checksum");
                }
            } catch(Exception ignored) {}

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

            MessageDigest md5Digest = MessageDigest.getInstance("MD5"); // 🟢 حساب البصمة للتحميل

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
                            byte[] data = response.getBody();
                            fos.write(data);
                            md5Digest.update(data); // 🟢 تحديث البصمة مع كل بلوك نحمله

                            downloadedBlocks++;
                            blockIndex++;

                            printDownloadProgressBar("📥 İndiriliyor:", downloadedBlocks, false);
                        }
                    } catch (Exception e) {
                        System.out.println("\n❌ " + partName + " indirilirken " + workerUrl + " düğümünde hata oluştu!");
                        moreParts = false;
                    }
                }

                boolean fileStillExists = true;
                try {
                    restTemplate.getForEntity(locateUrl, String.class);
                } catch (Exception e) { fileStillExists = false; }

                if (downloadedBlocks > 0 && fileStillExists) {
                    success = true;
                    printDownloadProgressBar("📥 İndiriliyor:", downloadedBlocks, true);
                    System.out.println();
                } else if (!fileStillExists) {
                    System.out.println("\n\n❌ KRİTİK HATA: İndirme işlemi sırasında dosya sunucudan silindi veya değiştirildi!");
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
                System.out.println("🎉 Dosya başarıyla indirildi: " + finalFile.getAbsolutePath());

                // 🟢 مطابقة البصمات (Verification)
                String localChecksum = bytesToHex(md5Digest.digest());
                if (!"YOK".equals(expectedChecksum)) {
                    if (localChecksum.equals(expectedChecksum)) {
                        System.out.println("✅ 🔒 Veri Bütünlüğü Doğrulandı (%100 Eşleşme) - MD5: " + localChecksum);
                    } else {
                        System.out.println("❌ ⚠️ DİKKAT: Veri Bütünlüğü Doğrulanamadı! Dosya bozulmuş olabilir.");
                    }
                }

                long endTime = System.currentTimeMillis();
                printPerformanceReport(finalFile.length(), startTime, endTime, "Download");
            }

        } catch (Exception e) {
            System.out.println("❌ Genel Hata: " + e.getMessage());
        }
    }

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

    private void printProgressBar(String action, int current, int total) {
        int barLength = 30;
        int percent = (int) ((current * 100.0) / total);
        int filledCount = (current * barLength) / total;

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filledCount) bar.append("█");
            else bar.append("░");
        }
        bar.append("]");

        System.out.print("\r" + action + " " + bar.toString() + " %" + percent + " | Blok: " + current + "/" + total);
    }

    private void printDownloadProgressBar(String action, int current, boolean isFinished) {
        int barLength = 30;
        StringBuilder bar = new StringBuilder("[");

        if (isFinished) {
            for (int i = 0; i < barLength; i++) bar.append("█");
        } else {
            int filledCount = current % barLength;
            if (filledCount == 0 && current > 0) filledCount = barLength;
            for (int i = 0; i < barLength; i++) {
                if (i < filledCount) bar.append("█");
                else bar.append("░");
            }
        }
        bar.append("]");

        String status = isFinished ? " (Tamamlandı)  " : "                ";
        System.out.print("\r" + action + " " + bar.toString() + " | Alınan Blok: " + current + status);
    }

    // 🟢 تحويل الـ Bytes إلى نص (للتشفير)
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}