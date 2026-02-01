package com.hdfs.datanode;

public class MasterContext {

    private static String masterBaseUrl;

    public static void set(String url) {
        masterBaseUrl = url;
    }

    public static String get() {
        return masterBaseUrl;
    }

    public static boolean isSet() {
        return masterBaseUrl != null;
    }
}
