package org.example.model.product;

import java.io.Serializable;
import java.time.LocalDate;

public class FoodProduct extends Product implements Serializable {
    private static final long serialVersionUID = 1L;
    private final LocalDate expirationDate;

    public FoodProduct(String name, double deliveryPrice, int quantity, LocalDate expirationDate) {
        super(name, deliveryPrice, quantity);
        if (expirationDate == null) {
            throw new IllegalArgumentException("Expiration date cannot be null");
        }
        this.expirationDate = expirationDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(expirationDate);
    }

    public boolean isNearExpiration(int warningDays) {
        LocalDate warningDate = LocalDate.now().plusDays(warningDays);
        return !isExpired() && LocalDate.now().isBefore(expirationDate) && !expirationDate.isAfter(warningDate);
    }

    @Override
    public double calculateSellingPrice() {
        double markup = 0.20; // 20% markup for food products
        double expirationDiscount = 0.10; // 10% discount for expired products
        double basePrice = getDeliveryPrice() * (1 + markup);
        if (isExpired()) {
            return basePrice * (1 - expirationDiscount);
        }
        return basePrice;
    }

    @Override
    public String toString() {
        return String.format("%s{name='%s', deliveryPrice=%s, quantity=%d, expirationDate=%s}",
            getClass().getSimpleName(), getName(), getDeliveryPrice(), getQuantity(), expirationDate);
    }
} 