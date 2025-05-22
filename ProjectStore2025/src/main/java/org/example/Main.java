package org.example;

import org.example.model.store.Store;
import org.example.model.store.Cashier;
import org.example.model.store.CashRegister;
import org.example.model.product.FoodProduct;
import org.example.model.product.NonFoodProduct;
import org.example.model.product.Product;
import org.example.model.product.ProductCategory;
import org.example.model.receipt.Receipt;
import org.example.exception.StoreException;
import org.example.config.StoreConfig;
import org.example.util.StoreLogger;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            // Print system information
            System.out.println("Current working directory: " + System.getProperty("user.dir"));
            System.out.println("User home directory: " + System.getProperty("user.home"));
            
            // Initialize receipts directory
            String receiptsDir = StoreConfig.getReceiptsDirectory();
            System.out.println("Receipts directory: " + receiptsDir);
            initializeReceiptsDirectory(receiptsDir);

            // Create store
            Store store = new Store(
                "Fantistiko",
                StoreConfig.getFoodMarkup(),
                StoreConfig.getNonFoodMarkup(),
                StoreConfig.getExpirationWarningDays(),
                StoreConfig.getExpirationDiscount()
            );

            // Add cashiers
            Cashier cashier1 = new Cashier("Ivan Georgiev", 2000.0);
            Cashier cashier2 = new Cashier("Teodor Ivanov", 2200.0);
            store.addCashier(cashier1);
            store.addCashier(cashier2);

            // Add registers
            CashRegister register1 = new CashRegister(1);
            CashRegister register2 = new CashRegister(2);
            store.addRegister(register1);
            store.addRegister(register2);

            // Assign cashiers to registers
            register1.setAssignedCashier(cashier1);
            register2.setAssignedCashier(cashier2);

            // Verify cashier assignments
            System.out.println("\nCashier Assignments:");
            System.out.println("Register 1: " + (register1.isAssigned() ? register1.getAssignedCashier().getName() : "No cashier assigned"));
            System.out.println("Register 2: " + (register2.isAssigned() ? register2.getAssignedCashier().getName() : "No cashier assigned"));

            // Add products
            FoodProduct milk = new FoodProduct("Milk", 2.0, 2.5, LocalDate.now().plusDays(7));
            FoodProduct bread = new FoodProduct("Bread", 1.5, 2.0, LocalDate.now().plusDays(5));
            NonFoodProduct soap = new NonFoodProduct("Soap", 1.0, 1.5);
            NonFoodProduct paper = new NonFoodProduct("Paper", 3.0, 4.0);

            // Set initial quantities
            milk.setQuantity(100);
            bread.setQuantity(50);
            soap.setQuantity(200);
            paper.setQuantity(150);

            // Add products to store with reorder points
            store.addProduct(milk, 100, 20, 50);
            store.addProduct(bread, 50, 10, 30);
            store.addProduct(soap, 200, 30, 100);
            store.addProduct(paper, 150, 25, 75);

            // Process sales
            Map<Product, Integer> sale1 = new HashMap<>();
            sale1.put(milk, 2);
            sale1.put(bread, 3);
            Receipt receipt1 = store.processSale(register1, sale1, 20.0);
            System.out.println("\nProcessed Sale 1:");
            System.out.println(receipt1);

            Map<Product, Integer> sale2 = new HashMap<>();
            sale2.put(soap, 5);
            sale2.put(paper, 2);
            Receipt receipt2 = store.processSale(register2, sale2, 30.0);
            System.out.println("\nProcessed Sale 2:");
            System.out.println(receipt2);

            // Print store information
            System.out.println("\nStore Information:");
            System.out.println("Total Revenue: $" + String.format("%.2f", store.getTotalRevenue()));
            System.out.println("Total Expenses: $" + String.format("%.2f", store.getTotalExpenses()));
            System.out.println("Profit: $" + String.format("%.2f", store.getProfit()));

            // Print analytics report
            System.out.println("\nAnalytics Report:");
            System.out.println(store.getAnalyticsReport());

            // Print inventory report
            System.out.println("\nInventory Report:");
            System.out.println(store.getInventoryReport());

        } catch (StoreException e) {
            System.err.println("Store error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void initializeReceiptsDirectory(String directory) {
        File dir = new File(directory);
        System.out.println("Initializing receipts directory: " + directory);
        
        if (!dir.exists()) {
            System.out.println("Creating receipts directory...");
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("Successfully created receipts directory");
            } else {
                System.err.println("Failed to create receipts directory");
                throw new RuntimeException("Failed to create receipts directory: " + directory);
            }
        } else {
            System.out.println("Receipts directory already exists");
        }

        if (!dir.canWrite()) {
            System.err.println("Receipts directory is not writable");
            throw new RuntimeException("Receipts directory is not writable: " + directory);
        }

        System.out.println("Receipts directory is ready for use");
    }
}
