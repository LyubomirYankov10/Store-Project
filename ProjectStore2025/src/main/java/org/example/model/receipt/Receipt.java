package org.example.model.receipt;

import org.example.model.product.Product;
import org.example.model.store.Cashier;
import org.example.exception.ReceiptException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class Receipt implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final AtomicInteger nextReceiptNumber = new AtomicInteger(1);
    private static final int MAX_RECEIPT_NUMBER = Integer.MAX_VALUE;
    private final int receiptNumber;
    private final Cashier cashier;
    private final LocalDateTime dateTime;
    private final Map<Product, Integer> items;
    private final double totalAmount;
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Receipt(Cashier cashier, Map<Product, Integer> items, double totalAmount) {
        if (cashier == null) {
            throw new ReceiptException("Cashier cannot be null");
        }
        if (items == null) {
            throw new ReceiptException("Items cannot be null");
        }
        if (items.isEmpty()) {
            throw new ReceiptException("Items cannot be empty");
        }
        if (totalAmount < 0) {
            throw new ReceiptException("Total amount cannot be negative");
        }

        this.receiptNumber = generateReceiptNumber();
        this.cashier = cashier;
        this.dateTime = LocalDateTime.now();
        this.items = new HashMap<>(items);
        this.totalAmount = totalAmount;
    }

    private static synchronized int generateReceiptNumber() {
        int current = nextReceiptNumber.get();
        if (current >= MAX_RECEIPT_NUMBER) {
            // Reset to 1 if we reach the maximum
            nextReceiptNumber.set(1);
            return 1;
        }
        return nextReceiptNumber.getAndIncrement();
    }

    public int getReceiptNumber() {
        return receiptNumber;
    }

    public Cashier getCashier() {
        return cashier;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public Map<Product, Integer> getItems() {
        return new HashMap<>(items);
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Receipt #").append(receiptNumber).append("\n");
        sb.append("Date: ").append(dateTime.format(dateFormatter)).append("\n");
        sb.append("Cashier: ").append(cashier.getName()).append("\n");
        sb.append("Items:\n");
        
        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            double price = product.getDeliveryPrice();
            double subtotal = price * quantity;
            sb.append(String.format("- %s x%d (%s each) = %s\n",
                product.getName(),
                quantity,
                currencyFormat.format(price),
                currencyFormat.format(subtotal)));
        }
        
        sb.append("Total Amount: ").append(currencyFormat.format(totalAmount));
        return sb.toString();
    }

    // Custom serialization methods
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Validate the deserialized object
        if (cashier == null || items == null || dateTime == null) {
            throw new ReceiptException("Invalid receipt data during deserialization");
        }
    }
} 