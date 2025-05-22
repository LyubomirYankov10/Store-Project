package org.example.model.product;

import java.time.LocalDate;

public class FoodProduct extends Product {
    private LocalDate expirationDate;
    private int quantity;

    public FoodProduct(String name, double deliveryPrice, double sellingPrice, LocalDate expirationDate) {
        super(name, deliveryPrice, sellingPrice, ProductCategory.FOOD);
        this.expirationDate = expirationDate;
        this.quantity = 0;
    }

    @Override
    public boolean isExpired() {
        return LocalDate.now().isAfter(expirationDate);
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.quantity = quantity;
    }

    @Override
    public boolean isNearExpiration(int warningDays) {
        return LocalDate.now().plusDays(warningDays).isAfter(expirationDate);
    }

    @Override
    public double calculateSellingPrice(double markup, int warningDays, double expirationDiscount) {
        double basePrice = getDeliveryPrice() * (1 + markup);
        if (isNearExpiration(warningDays)) {
            return basePrice * (1 - expirationDiscount);
        }
        return basePrice;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(", Expiration Date: %s, Quantity: %d",
            expirationDate, quantity);
    }
} 