package org.example.model.store;

import org.example.model.product.Product;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.text.NumberFormat;
import java.util.Locale;

public class CashRegister implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private final AtomicReference<Cashier> assignedCashier;
    private final Map<Product, Integer> currentTransaction;
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public CashRegister(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Register ID must be positive");
        }
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
        Cashier currentCashier = assignedCashier.get();
        if (currentCashier != null) {
            throw new IllegalStateException("Register already has an assigned cashier");
        }
        if (!assignedCashier.compareAndSet(null, cashier)) {
            throw new IllegalStateException("Register already has an assigned cashier");
        }
    }

    public void removeAssignedCashier() {
        Cashier currentCashier = assignedCashier.get();
        if (currentCashier != null) {
            if (!assignedCashier.compareAndSet(currentCashier, null)) {
                throw new IllegalStateException("Cashier assignment changed during removal");
            }
        }
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

    public void removeFromTransaction(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Integer currentQuantity = currentTransaction.get(product);
        if (currentQuantity == null || currentQuantity < quantity) {
            throw new IllegalStateException("Not enough quantity in transaction for product: " + product.getName());
        }
        if (currentQuantity == quantity) {
            currentTransaction.remove(product);
        } else {
            currentTransaction.put(product, currentQuantity - quantity);
        }
    }

    public void clearTransaction() {
        currentTransaction.clear();
    }

    public Map<Product, Integer> getCurrentTransaction() {
        return new ConcurrentHashMap<>(currentTransaction);
    }

    public double getCurrentTransactionTotal() {
        return currentTransaction.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().getDeliveryPrice() * entry.getValue())
                .sum();
    }

    @Override
    public String toString() {
        return String.format("CashRegister{id=%d, assignedCashier=%s, transactionSize=%d}",
            id, assignedCashier.get(), currentTransaction.size());
    }
} 