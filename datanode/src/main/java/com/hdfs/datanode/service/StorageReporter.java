package com.hdfs.datanode.service;

import com.hdfs.common.protocol.StorageReportRequest;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StorageReporter implements Runnable {

    private final String masterUrl;
    private final String workerUrl;
    private final String storagePath;

    public StorageReporter(String masterUrl, String workerUrl, String storagePath) {
        this.masterUrl = masterUrl;
        this.workerUrl = workerUrl;
        this.storagePath = storagePath;
    }

    @Override
    public void run() {
        RestTemplate restTemplate = new RestTemplate();

        while (true) {
            try {
                Thread.sleep(10000); // ⏱ كل 10 ثواني

                File dir = new File(storagePath);
                File[] files = dir.listFiles();

                long used = 0;
                List<String> blockIds = new ArrayList<>();

                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            used += f.length();
                            blockIds.add(f.getName()); // اسم الملف = blockId
                        }
                    }
                }

                long capacity = dir.getTotalSpace();

                StorageReportRequest report = new StorageReportRequest();
                report.setWorkerUrl(workerUrl);
                report.setCapacity(capacity);
                report.setUsed(used);
                report.setBlockIds(blockIds);

                restTemplate.postForObject(
                        masterUrl + "/api/worker/report",
                        report,
                        String.class
                );

                System.out.println("📊 Storage report sent. Blocks=" + blockIds.size());

            } catch (Exception e) {
                System.out.println("⚠️ Report failed: " + e.getMessage());
            }
        }
    }
}
