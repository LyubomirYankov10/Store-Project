package org.example.model.product;

import java.time.LocalDate;

public class NonFoodProduct extends Product {
    private int quantity;

    public NonFoodProduct(String name, double deliveryPrice, double sellingPrice) {
        super(name, deliveryPrice, sellingPrice, ProductCategory.NON_FOOD);
        this.quantity = 0;
    }

    @Override
    public boolean isExpired() {
        return false; // Non-food products don't expire
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
        return false; // Non-food products don't expire
    }

    @Override
    public double calculateSellingPrice(double markup, int warningDays, double expirationDiscount) {
        return getDeliveryPrice() * (1 + markup);
    }

    @Override
    public String toString() {
        return super.toString() + String.format(", Quantity: %d", quantity);
    }
} 