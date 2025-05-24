package org.example.model.store;

import org.example.model.product.Product;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class CashRegister {
    private final int id;
    private final AtomicReference<Cashier> assignedCashier;
    private final Map<Product, Integer> currentTransaction;

    public CashRegister(int id) {
        this.id = id;
        this.currentTransaction = new ConcurrentHashMap<>();
        this.assignedCashier = new AtomicReference<>(null);
    }

    public int getId() {
        return id;
    }

    public Cashier getAssignedCashier() {
        return assignedCashier.get();
    }

    public void setAssignedCashier(Cashier cashier) {
        if (cashier == null) {
            throw new IllegalArgumentException("Cashier cannot be null");
        }
        if (!assignedCashier.compareAndSet(null, cashier)) {
            throw new IllegalStateException("Register already has an assigned cashier");
        }
    }

    public void removeAssignedCashier() {
        assignedCashier.set(null);
    }

    public boolean isAssigned() {
        return assignedCashier.get() != null;
    }

    public void addToTransaction(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (product.getQuantity() < quantity) {
            throw new IllegalStateException("Not enough quantity available for product: " + product.getName());
        }
        currentTransaction.merge(product, quantity, Integer::sum);
    }

    public void clearTransaction() {
        currentTransaction.clear();
    }

    public Map<Product, Integer> getCurrentTransaction() {
        return new ConcurrentHashMap<>(currentTransaction);
    }
} 