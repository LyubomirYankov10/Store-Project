package org.example.model.product;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.NumberFormat;
import java.util.Locale;

public abstract class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final double deliveryPrice;
    private final AtomicInteger quantity;
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public Product(String name, double deliveryPrice, int quantity) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (deliveryPrice <= 0) {
            throw new IllegalArgumentException("Delivery price must be positive");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.name = name;
        this.deliveryPrice = deliveryPrice;
        this.quantity = new AtomicInteger(quantity);
    }

    public String getName() {
        return name;
    }

    public double getDeliveryPrice() {
        return deliveryPrice;
    }

    public int getQuantity() {
        return quantity.get();
    }

    public void addQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        quantity.addAndGet(amount);
    }

    public void removeQuantity(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        int currentQuantity = quantity.get();
        if (currentQuantity < amount) {
            throw new IllegalStateException("Not enough quantity available");
        }
        if (!quantity.compareAndSet(currentQuantity, currentQuantity - amount)) {
            throw new IllegalStateException("Quantity changed during removal");
        }
    }

    public abstract double calculateSellingPrice();

    public boolean isExpired() {
        return false; // Default implementation for non-food products
    }

    public boolean isNearExpiration(int warningDays) {
        return false; // Default implementation for non-food products
    }

    @Override
    public String toString() {
        return String.format("%s{name='%s', deliveryPrice=%s, quantity=%d}",
            getClass().getSimpleName(), name, currencyFormat.format(deliveryPrice), quantity.get());
    }
} 