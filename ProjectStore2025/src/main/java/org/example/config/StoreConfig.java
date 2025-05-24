package org.example.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class StoreConfig {
    private static final Properties properties = new Properties();
    private static final String DEFAULT_RECEIPTS_DIR = System.getProperty("user.home") + File.separator + "store_receipts";

    static {
        try {
            File configFile = new File("store.properties");
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                }
            } else {
                try (FileInputStream fis = new FileInputStream("src/main/resources/store.properties")) {
                    properties.load(fis);
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not load store.properties, using default values");
            e.printStackTrace();
        }

        if (!properties.containsKey("receipts.directory")) {
            properties.setProperty("receipts.directory", DEFAULT_RECEIPTS_DIR);
        }
    }

    public static String getReceiptsDirectory() {
        String dir = properties.getProperty("receipts.directory", DEFAULT_RECEIPTS_DIR).trim();
        dir = dir.replace("${user.home}", System.getProperty("user.home"));
        try {
            File file = new File(dir);
            return file.getCanonicalPath();
        } catch (IOException e) {
            return new File(dir).getAbsolutePath();
        }
    }

    public static double getFoodMarkup() {
        return Double.parseDouble(properties.getProperty("markup.food", "0.15"));
    }

    public static double getNonFoodMarkup() {
        return Double.parseDouble(properties.getProperty("markup.nonfood", "0.20"));
    }

    public static int getExpirationWarningDays() {
        return Integer.parseInt(properties.getProperty("expiration.warning.days", "7"));
    }

    public static double getExpirationDiscount() {
        return Double.parseDouble(properties.getProperty("expiration.discount", "0.20"));
    }
} 