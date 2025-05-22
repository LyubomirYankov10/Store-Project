package org.example.model.store;

import org.example.model.product.Product;
import java.util.HashMap;
import java.util.Map;

public class CashRegister {
    private int id;
    private Cashier assignedCashier;
    private Map<Product, Integer> currentTransaction;

    public CashRegister(int id) {
        this.id = id;
        this.currentTransaction = new HashMap<>();
        this.assignedCashier = null;
    }

    public int getId() {
        return id;
    }

    public Cashier getAssignedCashier() {
        return assignedCashier;
    }

    public void setAssignedCashier(Cashier cashier) {
        if (cashier == null) {
            throw new IllegalArgumentException("Cashier cannot be null");
        }
        if (this.assignedCashier != null) {
            throw new IllegalStateException("Register already has an assigned cashier");
        }
        this.assignedCashier = cashier;
    }

    public void removeAssignedCashier() {
        this.assignedCashier = null;
    }

    public boolean isAssigned() {
        return assignedCashier != null;
    }

    public void addToTransaction(Product product, int quantity) {
        if (product.getQuantity() < quantity) {
            throw new IllegalStateException("Not enough quantity available for product: " + product.getName());
        }
        currentTransaction.merge(product, quantity, Integer::sum);
    }

    public void clearTransaction() {
        currentTransaction.clear();
    }

    public Map<Product, Integer> getCurrentTransaction() {
        return new HashMap<>(currentTransaction);
    }
} 