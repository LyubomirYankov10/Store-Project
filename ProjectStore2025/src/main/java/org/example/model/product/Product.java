package org.example.model.product;

public abstract class Product {
    private String name;
    private double deliveryPrice;
    private double sellingPrice;
    private ProductCategory category;

    public Product(String name, double deliveryPrice, double sellingPrice, ProductCategory category) {
        this.name = name;
        this.deliveryPrice = deliveryPrice;
        this.sellingPrice = sellingPrice;
        this.category = category;
    }

    public abstract boolean isExpired();
    public abstract int getQuantity();
    public abstract boolean isNearExpiration(int warningDays);
    public abstract void setQuantity(int quantity);

    public String getName() {
        return name;
    }

    public double getDeliveryPrice() {
        return deliveryPrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public double calculateSellingPrice(double markup, int expirationWarningDays, double expirationDiscount) {
        double basePrice = sellingPrice * (1 + markup);
        if (isExpired()) {
            return basePrice * (1 - expirationDiscount);
        }
        return basePrice;
    }

    @Override
    public String toString() {
        return String.format("%s (Category: %s, Delivery Price: $%.2f, Selling Price: $%.2f)",
            name, category, deliveryPrice, sellingPrice);
    }
} 