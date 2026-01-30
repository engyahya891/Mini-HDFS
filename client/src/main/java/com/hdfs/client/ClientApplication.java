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
import org.springframework.web.client.HttpClientErrorException;

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
        System.out.println("Kullanılabilir komutlar : upload <dosya_yolu>, download <dosya_adı> , delete <dosya_adı> ,clear , exit");

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
                // ترجمة: مع السلامة! جاري الخروج...
                System.out.println("Güle güle! Çıkış yapılıyor...");
                break;
            }

            if ("upload".equalsIgnoreCase(command)) {
                if (parts.length < 2) {
                    // ترجمة: يرجى تحديد مسار الملف
                    System.out.println("⚠️ Lütfen dosya yolunu belirtin.");
                    continue;
                }

                // تنظيف المسار وحذف العلامات الزائدة
                String filePath = removeQuotes(parts[1]);
                uploadFile(filePath);

            } else if ("download".equalsIgnoreCase(command)) {
                if (parts.length < 2) {
                    // ترجمة: يرجى تحديد اسم الملف
                    System.out.println("⚠️ Lütfen dosya adını belirtin.");
                    continue;
                }

                String filename = removeQuotes(parts[1]);
                downloadFile(filename);

            } else if ("delete".equalsIgnoreCase(command)) {
            if (parts.length < 2) {
                System.out.println("⚠️ Lütfen silinecek dosya adını belirtin.");
                continue;
            }
            String filename = parts[1];
            deleteFileRequest(filename); // سننشئ هذه الدالة بالأسفل
        } else if ("clear".equalsIgnoreCase(command)) {
            clearScreen();
            System.out.println("✨ Console Cleared! ✨"); // رسالة تأكيد

        }
            else {
                // ترجمة: أمر غير معروف
                System.out.println("Bilinmeyen komut. (Yardım: upload, download, delete, clear , exit)");
            }
        }
    }
    // دالة مساعدة لحذف علامات التنصيص " إذا وضعها المستخدم
    // مثال: تحول "C:\My Folder\File.txt" إلى C:\My Folder\File.txt
    private String removeQuotes(String path) {
        path = path.trim();
        if (path.startsWith("\"") && path.endsWith("\"")) {
            return path.substring(1, path.length() - 1);
        }
        return path;
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
            System.out.println("❌ الملف غير موجود!");
            return;
        }

        long fileSize = file.length();
        long blockSize = 64 * 1024 * 1024; // حجم البلوك: 64 ميجابايت (يمكنك تصغيره للتجربة مثلاً 1 ميجا)

        // حساب عدد البلوكات الكلي
        int totalBlocks = (int) Math.ceil((double) fileSize / blockSize);
        System.out.println("📦 جاري تقسيم الملف إلى " + totalBlocks + " بلوك...");

        // فتح الملف للقراءة
        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[(int) blockSize];
            int bytesRead;
            int blockIndex = 0;

            // حلقة قراءة الملف (بلوك وراء بلوك)
            while ((bytesRead = fis.read(buffer)) != -1) {
                blockIndex++; // نبدأ من 1 أو 0 حسب اتفاقكم (هنا بدأت بزيادته فوراً ليصبح 1)

                System.out.println("\n🔹 معالجة البلوك رقم " + blockIndex + " / " + totalBlocks);

                // ---------------------------------------------------------
                // 1. سؤال الماستر: أين أرفع هذا البلوك؟
                // ---------------------------------------------------------
                // نرسل رقم البلوك الحالي للماستر
                // (نستخدم BlockAllocation كـ Request هنا للتبسيط، أو الكلاس الذي خصصه زميلك)
                com.hdfs.common.protocol.BlockAllocation request = new com.hdfs.common.protocol.BlockAllocation();
                request.setBlockIndex(blockIndex);

                // هنا الماستر سيرد بـ BlockAllocation تحتوي على "قائمة" سيرفرات
                com.hdfs.common.protocol.BlockAllocation response = restTemplate.postForObject(
                        "http://localhost:8080/api/file/allocate-block", request, com.hdfs.common.protocol.BlockAllocation.class);

                if (response == null || response.getWorkerUrls() == null || response.getWorkerUrls().isEmpty()) {
                    System.out.println("❌ فشل في تخصيص مكان للبلوك! الماستر لم يرد بسيرفرات.");
                    break;
                }

                // ---------------------------------------------------------
                // 2. حلقة التكرار (Replication Loop) 🔥
                // نرفع نفس البلوك لكل السيرفرات التي حددها الماستر
                // ---------------------------------------------------------
                for (String workerUrl : response.getWorkerUrls()) {
                    System.out.println("   🚀 رفع نسخة إلى: " + workerUrl);

                    try {
                        // 1. نقرأ البيانات بالحجم الحقيقي الذي قرأناه من الملف
                        // إذا كان البلوك ممتلئاً (64 ميجا)، فالمصفوفة ستكون 64 ميجا
                        // إذا كان البلوك الأخير (مثلاً 5 ميجا)، فالمصفوفة ستكون 5 ميجا
                        byte[] exactData = java.util.Arrays.copyOf(buffer, bytesRead);

                        // 2. ننشئ الـ Resource من البيانات الدقيقة
                        org.springframework.core.io.ByteArrayResource byteResource = new org.springframework.core.io.ByteArrayResource(exactData) {
                            @Override
                            public String getFilename() {
                                // اسم الملف: filename_part_1
                                return file.getName() + "_part_" + response.getBlockIndex();
                            }
                        };

                        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

                        org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
                        body.add("file", byteResource);

                        org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                                new org.springframework.http.HttpEntity<>(body, headers);

                        // إرسال
                        String result = restTemplate.postForObject(workerUrl + "/api/data/write", entity, String.class);
                        System.out.println("      ✅ Başarılı (Uploaded).");

                    } catch (Exception e) {
                        System.out.println("      ⚠️ Başarısız: " + e.getMessage());
                    }
                }
            }

            System.out.println("\n🎉 تمت عملية الرفع والنسخ الاحتياطي بنجاح!");

        } catch (Exception e) {
            System.out.println("❌ خطأ أثناء قراءة الملف أو رفعه: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void downloadFile(String filename) {
        System.out.println("🔄 Master'a soruluyor... (Asking Master...)");

        // 1️⃣ حدد المجلد الذي تريد الحفظ فيه (مثلاً على الـ C مباشرة)
        // ملاحظة: نستخدم \\ لأن العلامة الواحدة \ تعتبر رمزاً خاصاً في الجافا
        String targetFolder = "C:\\HDFS_Downloads\\";

        try {
            // التأكد من أن المجلد موجود، وإذا لم يكن موجوداً نقوم بإنشائه
            java.io.File directory = new java.io.File(targetFolder);
            if (!directory.exists()) {
                directory.mkdirs(); // ينشئ المجلد
            }

            // --- نفس كودك القديم للاتصال بالماستر ---
            String masterUrl = "http://localhost:8080/api/file/locate/" + filename;
            String workerUrl = restTemplate.getForObject(masterUrl, String.class);

            if ("NOT_FOUND".equals(workerUrl)) {
                System.out.println("⛔ Dosya Master kayıtlarında yok! (File not found)");
                return;
            }

            System.out.println("📍 Dosya bulundu: " + workerUrl);
            System.out.println("⬇️ İndiriliyor... (Downloading...)");

            // --- نفس كودك القديم للتحميل من الووركر ---
            String downloadUrl = workerUrl + "/api/data/read/" + filename;
            byte[] fileBytes = restTemplate.getForObject(downloadUrl, byte[].class);

            // 2️⃣ التغيير هنا: دمج مسار المجلد مع اسم الملف
            // استخدمنا الاسم الأصلي (filename) بدلاً من "downloaded_" ليكون أرتب
            java.nio.file.Path fullPath = java.nio.file.Paths.get(targetFolder + filename);

            // حفظ الملف في المسار الجديد
            java.nio.file.Files.write(fullPath, fileBytes);

            // طباعة مكان الحفظ الجديد للمستخدم
            System.out.println("🎉 İndirme başarılı! Dosya şuraya kaydedildi: " + fullPath.toAbsolutePath());

        } catch (Exception e) {
            System.out.println("❌ Hata: " + e.getMessage());
        }
    }
    private void deleteFileRequest(String filename) {
        System.out.println("🗑️ Deleting " + filename + "...");
        try {
            // إرسال طلب الحذف للماستر
            // لاحظ أننا نستخدم restTemplate.delete() لكنها لا ترجع قيمة نصية بسهولة
            // لذلك سنستخدم exchange لاستقبال الرد

            String masterUrl = "http://localhost:8080/api/file/delete/" + filename;

            // إرسال طلب DELETE
            restTemplate.delete(masterUrl);

            // (في الـ RestTemplate البسيط، دالة delete void،
            // إذا لم يحدث Exception فهذا يعني النجاح)
            System.out.println("✅ File deleted successfully!");

        } catch (Exception e) {
            System.out.println("❌ Failed to delete: " + e.getMessage());
        }
    }
    // دالة لتنظيف الشاشة
    private void clearScreen() {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                // إذا كان ويندوز، نستخدم ProcessBuilder لتشغيل أمر cls
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // إذا كان لينكس أو ماك، نستخدم أكواد ANSI
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // في حالة الفشل (مثلاً داخل IntelliJ)، نطبع أسطر فارغة كحل بديل
            for (int i = 0; i < 50; i++) System.out.println();
        }
    }
}
