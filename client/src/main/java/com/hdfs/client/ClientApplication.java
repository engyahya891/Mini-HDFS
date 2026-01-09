package com.hdfs.client;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.Scanner;

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
        System.out.println("Kullanılabilir komutlar : upload <dosya_yolu>, exit");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            String[] parts = input.split(" ");
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
            } else {
                System.out.println("Bilinmeyen Komut.");
            }
        }
    }

    private void uploadFile(String path) {
        System.out.println("TODO: Ana sonucuya yükleme isteği gönderilyor : " + path);
        // هنا سنكتب كود الاتصال بالماستر لاحقاً
        // الخطوة القادمة:
        // 1. سؤال الماستر: أين أرفع هذا الملف؟
        // 2. تقطيع الملف.
        // 3. الإرسال للووركر.
    }
}