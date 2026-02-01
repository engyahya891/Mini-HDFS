package com.hdfs.datanode;

import com.hdfs.common.protocol.WorkerRegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;
import com.hdfs.datanode.MasterContext;

import java.io.File;
import java.util.Scanner;

@SpringBootApplication
public class DataNodeApplication implements CommandLineRunner {

    @Value("${server.port}")
    private int serverPort;

    @Override
    public void run(String... args) {

        System.out.println("=========================================");
        System.out.println("   HDFS DATA NODE (WORKER) STARTING...   ");
        System.out.println("=========================================");

        Scanner scanner = new Scanner(System.in);
        System.out.print("✍️ Lütfen Master IP adresini girin: ");
        String masterIp = scanner.nextLine().trim();

        if (masterIp.isEmpty()) {
            System.out.println("❌ Master IP girilmedi! Çıkılıyor...");
            System.exit(1);
        }

        String masterBaseUrl = "http://" + masterIp + ":8080";
        MasterContext.set(masterBaseUrl);

        String registerUrl = masterBaseUrl + "/api/worker/register";

        String storagePath = "./data/worker_" + serverPort;
        new File(storagePath).mkdirs();

        WorkerRegisterRequest request = new WorkerRegisterRequest();
        request.setPort(serverPort);
        request.setStoragePath(storagePath);

        RestTemplate restTemplate = new RestTemplate();
        System.out.println("📡 Bağlanılıyor: " + registerUrl);

        try {
            restTemplate.postForObject(registerUrl, request, String.class);
            System.out.println("✅ Worker başarıyla register edildi.");
        } catch (Exception e) {
            System.out.println("❌ Master'a bağlanılamadı!");
            System.out.println("Sebep: " + e.getMessage());
            System.exit(1);
        }
    }
}
