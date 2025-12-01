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
        System.out.println("Welcome to Mini-HDFS Client!");
        System.out.println("Available commands: upload <file_path>, exit");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine();
            String[] parts = input.split(" ");
            String command = parts[0];

            if ("exit".equalsIgnoreCase(command)) {
                break;
            } else if ("upload".equalsIgnoreCase(command)) {
                if (parts.length < 2) {
                    System.out.println("Error: Please specify file path.");
                    continue;
                }
                String filePath = parts[1];
                uploadFile(filePath);
            } else {
                System.out.println("Unknown command.");
            }
        }
    }

    private void uploadFile(String path) {
        System.out.println("TODO: Sending request to Master to upload: " + path);
        // هنا سنكتب كود الاتصال بالماستر لاحقاً
        // الخطوة القادمة:
        // 1. سؤال الماستر: أين أرفع هذا الملف؟
        // 2. تقطيع الملف.
        // 3. الإرسال للووركر.
    }
}