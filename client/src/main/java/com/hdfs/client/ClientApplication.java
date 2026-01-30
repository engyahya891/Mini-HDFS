package com.hdfs.client;

import com.hdfs.common.protocol.ClientUploadRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileInputStream;
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

    // تعريف حجم البلوك (يجب أن يطابق الماستر = 64 ميجا)
    private static final int BLOCK_SIZE = 64 * 1024 * 1024;

    private void uploadFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("❌ الملف غير موجود!");
            return;
        }

        System.out.println("🔄 الاتصال بالماستر للحصول على خطة التوزيع...");

        try {
            // 1. إرسال طلب للماستر والحصول على "الخطة" (قائمة البلوكات)
            String masterUrl = "http://localhost:8080/api/file/upload";
            ClientUploadRequest request = new ClientUploadRequest(file.getName(), file.length());

            // استقبال القائمة (Array) ثم تحويلها
            BlockAllocation[] responseArray = restTemplate.postForObject(masterUrl, request, BlockAllocation[].class);

            if (responseArray == null) {
                System.out.println("❌ فشل الحصول على خطة من الماستر.");
                return;
            }

            System.out.println("✅ تم استلام الخطة! عدد القطع: " + responseArray.length);

            // 2. فتح الملف وبدء التقطيع 🔪
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[BLOCK_SIZE]; // الوعاء الذي سنغرف فيه البيانات
                int bytesRead;
                int currentBlockIndex = 0;

                // حلقة تكرارية: اقرأ 64 ميجا في كل مرة
                while ((bytesRead = fis.read(buffer)) != -1) {

                    // الحصول على وجهة هذا البلوك من الخطة
                    BlockAllocation allocation = responseArray[currentBlockIndex];
                    String workerUrl = allocation.getWorkerUrl();

                    System.out.println("   ⬆️ رفع القطعة #" + currentBlockIndex + " إلى: " + workerUrl);

                    // تجهيز البيانات (قص البلوك الأخير إذا كان أصغر من 64 ميجا)
                    byte[] actualData = buffer;
                    if (bytesRead < BLOCK_SIZE) {
                        // إذا قرأنا 10 ميجا فقط، نقص المصفوفة لتصبح 10 ميجا
                        actualData = java.util.Arrays.copyOf(buffer, bytesRead);
                    }

                    // 3. إرسال القطعة للووركر
                    // 💡 ملاحظة ذكية: سنسمي الملف "video.mp4_blk_0" ليحفظه الووركر بهذا الاسم
                    String blockName = file.getName() + "_blk_" + currentBlockIndex;

                    // استخدام Multipart لإرسال الملف (مثل الكود القديم لكن مع تغيير الرابط والاسم)
                    uploadBlockToWorker(workerUrl, blockName, actualData);

                    currentBlockIndex++;
                }
            }
            System.out.println("🎉 تمت عملية الرفع الموزع بنجاح!");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ خطأ أثناء الرفع: " + e.getMessage());
        }
    }

    // دالة مساعدة لإرسال البايتات للووركر (تشبه كود الرفع القديم)
    private void uploadBlockToWorker(String workerBaseUrl, String blockName, byte[] data) {
        try {
            String url = workerBaseUrl + "/api/data/upload";

            // تجهيز الهيدر والملف
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

            org.springframework.util.MultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            // نستخدم ByteArrayResource لتغليف البايتات كملف
            body.add("file", new org.springframework.core.io.ByteArrayResource(data) {
                @Override
                public String getFilename() {
                    return blockName; // هذا الاسم الذي سيحفظه الووركر
                }
            });

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, Object>> requestEntity = new org.springframework.http.HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, requestEntity, String.class);

        } catch (Exception e) {
            throw new RuntimeException("فشل رفع البلوك للووركر: " + workerBaseUrl);
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
