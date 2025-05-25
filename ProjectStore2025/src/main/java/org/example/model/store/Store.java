package org.example.model.store;

import org.example.model.product.Product;
import org.example.model.product.ProductCategory;
import org.example.model.receipt.Receipt;
import org.example.model.analytics.StoreAnalytics;
import org.example.model.inventory.InventoryManager;
import org.example.exception.StoreException;
import org.example.exception.ReceiptException;
import org.example.exception.ProductException;
import org.example.util.StoreLogger;
import org.example.config.StoreConfig;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Store {
    private final String name;
    private final double foodMarkup;
    private final double nonFoodMarkup;
    private final int expirationWarningDays;
    private final double expirationDiscount;
    private final List<Cashier> cashiers;
    private final List<CashRegister> registers;
    private final List<Product> products;
    private final List<Receipt> receipts;
    private final AtomicReference<Double> totalRevenue;
    private final AtomicReference<Double> totalExpenses;
    private final StoreAnalytics analytics;
    private final InventoryManager inventory;

    public Store(String name, double foodMarkup, double nonFoodMarkup, 
                int expirationWarningDays, double expirationDiscount) {
        this.name = name;
        this.foodMarkup = foodMarkup;
        this.nonFoodMarkup = nonFoodMarkup;
        this.expirationWarningDays = expirationWarningDays;
        this.expirationDiscount = expirationDiscount;
        this.cashiers = new CopyOnWriteArrayList<>();
        this.registers = new CopyOnWriteArrayList<>();
        this.products = new CopyOnWriteArrayList<>();
        this.receipts = new CopyOnWriteArrayList<>();
        this.totalRevenue = new AtomicReference<>(0.0);
        this.totalExpenses = new AtomicReference<>(0.0);
        this.analytics = new StoreAnalytics();
        this.inventory = new InventoryManager();
        
        StoreLogger.info("Store '" + name + "' created with food markup: " + foodMarkup + 
            ", non-food markup: " + nonFoodMarkup);
    }

    public void addCashier(Cashier cashier) {
        if (cashier == null) {
            throw new StoreException("Cannot add null cashier");
        }
        cashiers.add(cashier);
        totalExpenses.updateAndGet(current -> current + cashier.getMonthlySalary());
        analytics.addExpense(cashier.getMonthlySalary());
        StoreLogger.info("Cashier '" + cashier.getName() + "' added to store");
    }

    public void removeCashier(Cashier cashier) {
        if (cashier == null) {
            throw new StoreException("Cannot remove null cashier");
        }
        if (cashier.isAssignedToRegister()) {
            cashier.getAssignedRegister().removeAssignedCashier();
        }
        cashiers.remove(cashier);
        totalExpenses.updateAndGet(current -> current - cashier.getMonthlySalary());
        analytics.addExpense(-cashier.getMonthlySalary());
        StoreLogger.info("Cashier '" + cashier.getName() + "' removed from store");
    }

    public void addRegister(CashRegister register) {
        if (register == null) {
            throw new StoreException("Cannot add null register");
        }
        registers.add(register);
        StoreLogger.info("Register '" + register.getId() + "' added to store");
    }

    public void removeRegister(CashRegister register) {
        if (register == null) {
            throw new StoreException("Cannot remove null register");
        }
        if (register.isAssigned()) {
            register.getAssignedCashier().removeAssignedRegister();
        }
        registers.remove(register);
        StoreLogger.info("Register '" + register.getId() + "' removed from store");
    }

    public void addProduct(Product product, int initialStock, int reorderPoint, int reorderQuantity) {
        if (product == null) {
            throw new StoreException("Cannot add null product");
        }
        products.add(product);
        double expense = product.getDeliveryPrice() * initialStock;
        totalExpenses.updateAndGet(current -> current + expense);
        analytics.addExpense(expense);
        inventory.addProduct(product, initialStock, reorderPoint, reorderQuantity);
        StoreLogger.info("Product '" + product.getName() + "' added to store with " + initialStock + " units");
    }

    public void removeProduct(Product product) {
        if (product == null) {
            throw new StoreException("Cannot remove null product");
        }
        products.remove(product);
        StoreLogger.info("Product '" + product.getName() + "' removed from store");
    }

    public Receipt processSale(CashRegister register, Map<Product, Integer> items, double payment) {
        if (register == null) {
            throw new StoreException("Register cannot be null");
        }
        if (items == null || items.isEmpty()) {
            throw new StoreException("Items cannot be null or empty");
        }
        if (payment < 0) {
            throw new StoreException("Payment cannot be negative");
        }

        if (!register.isAssigned()) {
            throw new StoreException("No cashier assigned to register");
        }

        double totalAmount = calculateTotalAmount(items);

        if (payment < totalAmount) {
            throw new StoreException("Insufficient payment. Required: " + totalAmount + ", Provided: " + payment);
        }

        Map<Product, Integer> transactionItems = new HashMap<>(items);
        
        try {
            for (Map.Entry<Product, Integer> entry : transactionItems.entrySet()) {
                Product product = entry.getKey();
                int quantity = entry.getValue();
                if (inventory.getStockLevel(product) < quantity) {
                    throw new StoreException("Insufficient stock for product: " + product.getName());
                }
            }

            for (Map.Entry<Product, Integer> entry : transactionItems.entrySet()) {
                Product product = entry.getKey();
                int quantity = entry.getValue();
                inventory.updateStock(product, -quantity);
            }

            Receipt receipt = new Receipt(register.getAssignedCashier(), transactionItems, totalAmount);
            receipts.add(receipt);
            totalRevenue.updateAndGet(current -> current + totalAmount);
            analytics.addReceipt(receipt);

            saveReceiptToFile(receipt);
            StoreLogger.info("Sale processed successfully. Receipt #" + receipt.getReceiptNumber());
            
            return receipt;
        } catch (Exception e) {
            try {
                for (Map.Entry<Product, Integer> entry : transactionItems.entrySet()) {
                    Product product = entry.getKey();
                    int quantity = entry.getValue();
                    inventory.updateStock(product, quantity);
                }
            } catch (ProductException pe) {
                StoreLogger.error("Failed to rollback inventory changes", pe);
            }
            StoreLogger.error("Failed to process sale", e);
            throw new StoreException("Failed to process sale: " + e.getMessage(), e);
        }
    }

    private double calculateTotalAmount(Map<Product, Integer> items) {
        return items.entrySet().stream()
                .mapToDouble(entry -> entry.getKey().calculateSellingPrice() * entry.getValue())
                .sum();
    }

    public double getTotalRevenue() {
        return Math.round(totalRevenue.get() * 100.0) / 100.0;
    }

    public double getTotalExpenses() {
        return Math.round(totalExpenses.get() * 100.0) / 100.0;
    }

    public double getProfit() {
        return Math.round((getTotalRevenue() - getTotalExpenses()) * 100.0) / 100.0;
    }

    public String getName() {
        return name;
    }

    public List<Cashier> getCashiers() {
        return new ArrayList<>(cashiers);
    }

    public List<CashRegister> getRegisters() {
        return new ArrayList<>(registers);
    }

    public List<Product> getProducts() {
        return new ArrayList<>(products);
    }

    public List<Receipt> getReceipts() {
        return new ArrayList<>(receipts);
    }

    public String getAnalyticsReport() {
        return analytics.generateReport();
    }

    public String getInventoryReport() {
        return inventory.generateReport();
    }

    private void saveReceiptToFile(Receipt receipt) throws ReceiptException {
        String receiptsDir = StoreConfig.getReceiptsDirectory();
        File dir = new File(receiptsDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new ReceiptException("Failed to create receipts directory: " + receiptsDir);
        }

        String fileName = String.format("receipt_%d.ser", receipt.getReceiptNumber());
        File file = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(receipt);
        } catch (IOException e) {
            throw new ReceiptException("Failed to save receipt: " + e.getMessage(), e);
        }
    }
} 