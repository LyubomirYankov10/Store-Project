package org.example.model.store;

import org.example.model.product.Product;
import org.example.model.product.ProductCategory;
import org.example.model.receipt.Receipt;
import org.example.model.analytics.StoreAnalytics;
import org.example.model.inventory.InventoryManager;
import org.example.exception.StoreException;
import org.example.exception.ReceiptException;
import org.example.util.StoreLogger;
import org.example.config.StoreConfig;

import java.io.*;
import java.util.*;

public class Store {
    private String name;
    private double foodMarkup;
    private double nonFoodMarkup;
    private int expirationWarningDays;
    private double expirationDiscount;
    private List<Cashier> cashiers;
    private List<CashRegister> registers;
    private List<Product> products;
    private List<Receipt> receipts;
    private double totalRevenue;
    private double totalExpenses;
    private StoreAnalytics analytics;
    private InventoryManager inventory;

    public Store(String name, double foodMarkup, double nonFoodMarkup, 
                int expirationWarningDays, double expirationDiscount) {
        this.name = name;
        this.foodMarkup = foodMarkup;
        this.nonFoodMarkup = nonFoodMarkup;
        this.expirationWarningDays = expirationWarningDays;
        this.expirationDiscount = expirationDiscount;
        this.cashiers = new ArrayList<>();
        this.registers = new ArrayList<>();
        this.products = new ArrayList<>();
        this.receipts = new ArrayList<>();
        this.totalRevenue = 0.0;
        this.totalExpenses = 0.0;
        this.analytics = new StoreAnalytics();
        this.inventory = new InventoryManager();
        
        StoreLogger.info("Store '" + name + "' created with food markup: " + foodMarkup + 
            ", non-food markup: " + nonFoodMarkup);
    }

    // Cashier management
    public void addCashier(Cashier cashier) {
        if (cashier == null) {
            throw new StoreException("Cannot add null cashier");
        }
        cashiers.add(cashier);
        totalExpenses += cashier.getMonthlySalary();
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
        totalExpenses -= cashier.getMonthlySalary();
        analytics.addExpense(-cashier.getMonthlySalary());
        StoreLogger.info("Cashier '" + cashier.getName() + "' removed from store");
    }

    // Register management
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

    // Product management
    public void addProduct(Product product, int initialStock, int reorderPoint, int reorderQuantity) {
        if (product == null) {
            throw new StoreException("Cannot add null product");
        }
        products.add(product);
        totalExpenses += product.getDeliveryPrice() * initialStock;
        analytics.addExpense(product.getDeliveryPrice() * initialStock);
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

    // Sales operations
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

        double totalAmount = 0.0;
        for (Map.Entry<Product, Integer> entry : items.entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            
            if (product.isExpired()) {
                throw new StoreException("Cannot sell expired product: " + product.getName());
            }

            if (inventory.getStockLevel(product) < quantity) {
                throw new StoreException("Insufficient stock for product: " + product.getName());
            }

            double markup = product.getCategory() == ProductCategory.FOOD ? foodMarkup : nonFoodMarkup;
            double price = product.calculateSellingPrice(markup, expirationWarningDays, expirationDiscount);
            totalAmount += price * quantity;
            
            inventory.updateStock(product, -quantity);
        }

        if (payment < totalAmount) {
            throw new StoreException("Insufficient payment. Required: " + totalAmount + ", Provided: " + payment);
        }

        Receipt receipt = new Receipt(register.getAssignedCashier(), items, totalAmount);
        receipts.add(receipt);
        totalRevenue += totalAmount;
        analytics.addReceipt(receipt);

        try {
            saveReceiptToFile(receipt);
            StoreLogger.info("Sale processed successfully. Receipt #" + receipt.getReceiptNumber());
        } catch (Exception e) {
            StoreLogger.error("Failed to save receipt", e);
            throw new ReceiptException("Failed to save receipt", e);
        }

        return receipt;
    }

    // Financial calculations
    public double getTotalRevenue() {
        return totalRevenue;
    }

    public double getTotalExpenses() {
        return totalExpenses;
    }

    public double getProfit() {
        return totalRevenue - totalExpenses;
    }

    // Analytics
    public String getAnalyticsReport() {
        return analytics.generateReport();
    }

    public String getInventoryReport() {
        return inventory.generateInventoryReport();
    }

    // Receipt management
    private void saveReceiptToFile(Receipt receipt) {
        String directory = StoreConfig.getReceiptsDirectory();
        System.out.println("Attempting to save receipt to directory: " + directory);
        StoreLogger.info("Attempting to save receipt to directory: " + directory);
        
        File dir = new File(directory);
        
        // Create directory if it doesn't exist
        if (!dir.exists()) {
            System.out.println("Receipts directory does not exist, attempting to create: " + directory);
            StoreLogger.info("Receipts directory does not exist, attempting to create: " + directory);
            boolean created = dir.mkdirs();
            if (!created) {
                String error = "Failed to create receipts directory: " + directory;
                System.err.println(error);
                StoreLogger.error(error, new ReceiptException(error));
                throw new ReceiptException(error);
            }
            System.out.println("Successfully created receipts directory: " + directory);
            StoreLogger.info("Successfully created receipts directory: " + directory);
        }

        // Check if directory is writable
        if (!dir.canWrite()) {
            String error = "Receipts directory is not writable: " + directory;
            System.err.println(error);
            StoreLogger.error(error, new ReceiptException(error));
            throw new ReceiptException(error);
        }

        String fileName = directory + File.separator + "receipt_" + receipt.getReceiptNumber() + ".txt";
        File receiptFile = new File(fileName);
        System.out.println("Attempting to save receipt to file: " + fileName);
        StoreLogger.info("Attempting to save receipt to file: " + fileName);
        
        try {
            // Create parent directories if they don't exist
            File parentDir = receiptFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                System.out.println("Creating parent directories for: " + fileName);
                StoreLogger.info("Creating parent directories for: " + fileName);
                boolean created = parentDir.mkdirs();
                if (!created) {
                    String error = "Failed to create parent directories for receipt file: " + fileName;
                    System.err.println(error);
                    StoreLogger.error(error, new ReceiptException(error));
                    throw new ReceiptException(error);
                }
            }

            // Save receipt to file
            try (PrintWriter writer = new PrintWriter(new FileWriter(receiptFile))) {
                String receiptContent = receipt.toString();
                System.out.println("Writing receipt content: " + receiptContent);
                writer.println(receiptContent);
                writer.flush();
                System.out.println("Successfully saved receipt #" + receipt.getReceiptNumber() + 
                    " to file: " + fileName);
                StoreLogger.info("Successfully saved receipt #" + receipt.getReceiptNumber() + 
                    " to file: " + fileName);
            }
        } catch (IOException e) {
            String error = "Failed to save receipt to file: " + fileName;
            System.err.println(error);
            System.err.println("Error details: " + e.getMessage());
            e.printStackTrace();
            StoreLogger.error(error, e);
            throw new ReceiptException(error, e);
        }
    }

    public Receipt loadReceiptFromFile(int receiptNumber) {
        String fileName = StoreConfig.getReceiptsDirectory() + File.separator + 
            "receipt_" + receiptNumber + ".txt";
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            // TODO: Implement proper receipt parsing
            throw new UnsupportedOperationException("Receipt loading not implemented yet");
        } catch (IOException e) {
            throw new ReceiptException("Failed to load receipt from file: " + fileName, e);
        }
    }

    // Getters
    public String getName() {
        return name;
    }

    public List<Cashier> getCashiers() {
        return Collections.unmodifiableList(cashiers);
    }

    public List<CashRegister> getRegisters() {
        return Collections.unmodifiableList(registers);
    }

    public List<Product> getProducts() {
        return Collections.unmodifiableList(products);
    }

    public List<Receipt> getReceipts() {
        return Collections.unmodifiableList(receipts);
    }
} 