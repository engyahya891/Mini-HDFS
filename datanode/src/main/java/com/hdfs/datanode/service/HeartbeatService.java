package com.hdfs.datanode.service;

import com.hdfs.common.protocol.HeartbeatRequest;
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

    // 🟢 Master adresinin doğru olduğundan emin olun
    private String masterUrl = "http://localhost:8080";

    private final RestTemplate restTemplate = new RestTemplate();

    // ⏱️ Her 5 saniyede bir otomatik çalışır
    @Scheduled(fixedRate = 5000)
    public void sendHeartbeat() {
        try {
            // 1. Depolama klasörünü belirle ve disk alanını hesapla
            File storageDir = new File("./data/worker_" + port);
            if (!storageDir.exists()) storageDir.mkdirs();

            long totalSpace = storageDir.getTotalSpace(); // toplam alan
            long freeSpace  = storageDir.getFreeSpace();  // boş alan
            long usedSpace  = totalSpace - freeSpace;     // kullanılan alan

            // 2. Mevcut cihazın IP adresini al (master'a göndermek için)
            String myIp = InetAddress.getLocalHost().getHostAddress();
            String myUrl = "http://" + myIp + ":" + port;

            // Eğer her zaman localhost kullanıyorsan, yukarıdaki satırlar yerine:
            // String myUrl = "http://127.0.0.1:" + port;

            // 3. Heartbeat raporunu hazırla (URL, kullanılan alan, toplam alan)
            HeartbeatRequest request =
                    new HeartbeatRequest(myUrl, usedSpace, totalSpace);

            // 4. Master node'a gönder
            restTemplate.postForObject(
                    masterUrl + "/api/worker/heartbeat",
                    request,
                    Void.class
            );

            // Kontrol amaçlı çıktı (isteğe bağlı)
            System.out.println(
                    "💓 Heartbeat gönderildi: " + myUrl +
                            " | Kullanılan: " + (usedSpace / 1024 / 1024) + " MB"
            );

        } catch (Exception e) {
            System.err.println("❌ Heartbeat gönderimi başarısız: " + e.getMessage());
        }
    }
}
