package org.example.model.store;

public class Cashier {
    private String name;
    private double monthlySalary;
    private CashRegister assignedRegister;

    public Cashier(String name, double monthlySalary) {
        this.name = name;
        this.monthlySalary = monthlySalary;
        this.assignedRegister = null;
    }

    public void assignToRegister(CashRegister register) {
        if (register == null) {
            throw new IllegalArgumentException("Register cannot be null");
        }
        if (this.assignedRegister != null) {
            throw new IllegalStateException("Cashier is already assigned to a register");
        }
        this.assignedRegister = register;
    }

    public void removeAssignedRegister() {
        this.assignedRegister = null;
    }

    public boolean isAssignedToRegister() {
        return assignedRegister != null;
    }

    public CashRegister getAssignedRegister() {
        return assignedRegister;
    }

    public String getName() {
        return name;
    }

    public double getMonthlySalary() {
        return monthlySalary;
    }
} 