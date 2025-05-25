package org.example.model.product;

import java.io.Serializable;

public class NonFoodProduct extends Product implements Serializable {
    private static final long serialVersionUID = 1L;

    public NonFoodProduct(String name, double deliveryPrice, int quantity) {
        super(name, deliveryPrice, quantity);
    }

    @Override
    public double calculateSellingPrice() {
        double markup = 0.15; // 15% markup for non-food products
        return getDeliveryPrice() * (1 + markup);
    }

    @Override
    public String toString() {
        return String.format("%s{name='%s', deliveryPrice=%s, quantity=%d}",
            getClass().getSimpleName(), getName(), getDeliveryPrice(), getQuantity());
    }
} 