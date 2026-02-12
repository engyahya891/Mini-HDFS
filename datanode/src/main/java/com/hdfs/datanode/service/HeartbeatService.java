package com.hdfs.datanode.service;

import com.hdfs.common.protocol.HeartbeatRequest;
import com.hdfs.datanode.MasterContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.net.InetAddress;

@Service
public class HeartbeatService {

    @Value("${server.port}")
    private String port;

    private final RestTemplate restTemplate = new RestTemplate();

    // ⏱️ Her 5 saniyede bir çalışır
    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {

        // Master adresi ayarlanmamışsa hiçbir işlem yapma
        if (!MasterContext.isSet()) {
            return;
        }

        try {
            // 1. Depolama alanını hesapla
            File storageDir = new File("./data/worker_" + port);
            if (!storageDir.exists()) storageDir.mkdirs();

            long totalSpace = storageDir.getTotalSpace();
            long freeSpace  = storageDir.getFreeSpace();
            long usedSpace  = totalSpace - freeSpace;

            // 2. Mevcut IP adresini al
            String myIp = InetAddress.getLocalHost().getHostAddress();
            String myUrl = "http://" + myIp + ":" + port;

            // 3. Heartbeat isteğini hazırla
            HeartbeatRequest request = new HeartbeatRequest(myUrl, usedSpace, totalSpace);

            // 4. Master'a gönder
            restTemplate.postForObject(
                    MasterContext.get() + "/api/worker/heartbeat",
                    request,
                    Void.class
            );

            System.out.println("💓 Heartbeat gönderildi: "
                    + MasterContext.get()
                    + " | Kullanılan Alan: "
                    + (usedSpace / 1024 / 1024)
                    + " MB");

        } catch (Exception e) {

            System.err.println("⚠️ Heartbeat başarısız: "
                    + MasterContext.get()
                    + " adresine bağlanılamadı.");
        }
    }
}
