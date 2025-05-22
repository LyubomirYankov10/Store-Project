package org.example.model.receipt;

import org.example.model.product.Product;
import org.example.model.store.Cashier;
import org.example.exception.ReceiptException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

public class Receipt implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int receiptCounter = 0;

    private final int receiptNumber;
    private final Cashier cashier;
    private final LocalDateTime dateTime;
    private final Map<Product, Integer> items;
    private final double totalAmount;

    public Receipt(Cashier cashier, Map<Product, Integer> items, double totalAmount) {
        if (cashier == null) {
            throw new ReceiptException("Cashier cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new ReceiptException("Items cannot be null or empty");
        }
        if (totalAmount < 0) {
            throw new ReceiptException("Total amount cannot be negative");
        }

        this.receiptNumber = ++receiptCounter;
        this.cashier = cashier;
        this.dateTime = LocalDateTime.now();
        this.items = new HashMap<>(items);
        this.totalAmount = totalAmount;
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
        sb.append("Date: ").append(dateTime).append("\n");
        sb.append("Cashier: ").append(cashier.getName()).append("\n");
        sb.append("Items:\n");
        
        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            double price = product.getDeliveryPrice();
            double itemTotal = price * quantity;
            sb.append(String.format("- %s x%d (%.2f each) = %.2f\n", 
                product.getName(), quantity, price, itemTotal));
        }
        
        sb.append("Total Amount: ").append(String.format("%.2f", totalAmount));
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