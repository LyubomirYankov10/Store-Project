package org.example.exception;

public class ReceiptException extends StoreException {
    public ReceiptException(String message) {
        super(message);
    }

    public ReceiptException(String message, Throwable cause) {
        super(message, cause);
    }
} 