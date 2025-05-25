package org.example.model.store;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Cashier implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final AtomicInteger nextId = new AtomicInteger(1);
    
    private final int id;
    private String name;
    private double monthlySalary;
    private final AtomicReference<CashRegister> assignedRegister;

    public Cashier(String name, double monthlySalary) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cashier name cannot be null or empty");
        }
        if (monthlySalary <= 0) {
            throw new IllegalArgumentException("Monthly salary must be positive");
        }
        
        this.id = nextId.getAndIncrement();
        this.name = name;
        this.monthlySalary = monthlySalary;
        this.assignedRegister = new AtomicReference<>(null);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cashier name cannot be null or empty");
        }
        this.name = name;
    }

    public double getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(double monthlySalary) {
        if (monthlySalary <= 0) {
            throw new IllegalArgumentException("Monthly salary must be positive");
        }
        this.monthlySalary = monthlySalary;
    }

    public CashRegister getAssignedRegister() {
        return assignedRegister.get();
    }

    public void setAssignedRegister(CashRegister register) {
        if (register == null) {
            throw new IllegalArgumentException("Register cannot be null");
        }
        CashRegister currentRegister = assignedRegister.get();
        if (currentRegister != null) {
            throw new IllegalStateException("Cashier is already assigned to a register");
        }
        if (!assignedRegister.compareAndSet(null, register)) {
            throw new IllegalStateException("Cashier is already assigned to a register");
        }
    }

    public void removeAssignedRegister() {
        CashRegister currentRegister = assignedRegister.get();
        if (currentRegister != null) {
            if (!assignedRegister.compareAndSet(currentRegister, null)) {
                throw new IllegalStateException("Register assignment changed during removal");
            }
        }
    }

    public boolean isAssignedToRegister() {
        return assignedRegister.get() != null;
    }

    @Override
    public String toString() {
        return String.format("Cashier{id=%d, name='%s', monthlySalary=%.2f, assignedRegister=%s}",
            id, name, monthlySalary, assignedRegister.get());
    }
} 