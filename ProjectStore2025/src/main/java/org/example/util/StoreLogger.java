package org.example.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StoreLogger {
    private static final String LOG_FILE = "store.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static PrintWriter logWriter;

    static {
        try {
            File logFile = new File(LOG_FILE);
            logWriter = new PrintWriter(new FileWriter(logFile, true));
            info("Logger initialized");
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void info(String message) {
        log("INFO", message);
    }

    public static void error(String message, Throwable t) {
        log("ERROR", message);
        if (t != null) {
            t.printStackTrace(logWriter);
        }
    }

    public static void warning(String message) {
        log("WARNING", message);
    }

    private static synchronized void log(String level, String message) {
        if (logWriter != null) {
            String timestamp = LocalDateTime.now().format(formatter);
            logWriter.println(timestamp + " [" + level + "] " + message);
            logWriter.flush();
        }
    }

    public static void close() {
        if (logWriter != null) {
            try {
                info("Logger shutting down");
                logWriter.close();
            } catch (Exception e) {
                System.err.println("Error closing logger: " + e.getMessage());
            }
        }
    }
} 