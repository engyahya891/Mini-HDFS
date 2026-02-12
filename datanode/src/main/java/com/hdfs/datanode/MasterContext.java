package com.hdfs.datanode;

public class MasterContext {

    // القيمة الافتراضية null، مما يعني أن النظام غير جاهز
    private static String masterBaseUrl = null;

    public static void set(String url) {
        // التأكد من عدم وجود / في النهاية
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        masterBaseUrl = url;
    }

    public static String get() {
        return masterBaseUrl;
    }

    // هذه الدالة هي "صمام الأمان"
    public static boolean isSet() {
        return masterBaseUrl != null && !masterBaseUrl.isEmpty();
    }
}