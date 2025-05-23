package org.example.model.analytics;

import org.example.model.product.FoodProduct;
import org.example.model.product.NonFoodProduct;
import org.example.model.product.Product;
import org.example.model.receipt.Receipt;
import org.example.model.store.Cashier;
import org.example.model.store.CashRegister;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StoreAnalyticsTest {
    private StoreAnalytics analytics;
    private Cashier cashier1;
    private Cashier cashier2;
    private CashRegister register1;
    private CashRegister register2;
    private FoodProduct milk;
    private FoodProduct bread;
    private NonFoodProduct soap;
    private NonFoodProduct paper;

    @BeforeEach
    void setUp() {
        analytics = new StoreAnalytics();
        
        cashier1 = new Cashier("John Doe", 2000.0);
        cashier2 = new Cashier("Jane Smith", 2200.0);
        register1 = new CashRegister(1);
        register2 = new CashRegister(2);
        
        milk = new FoodProduct("Milk", 2.0, 2.5, LocalDate.now().plusDays(7));
        bread = new FoodProduct("Bread", 1.5, 2.0, LocalDate.now().plusDays(5));
        soap = new NonFoodProduct("Soap", 1.0, 1.5);
        paper = new NonFoodProduct("Paper", 3.0, 4.0);

        // Add some initial expenses
        analytics.addExpense(2000.0); // Cashier 1 salary
        analytics.addExpense(2200.0); // Cashier 2 salary
    }

    @Test
    void testAddReceipt() {
        Map<Product, Integer> items = new HashMap<>();
        items.put(milk, 2);
        items.put(bread, 3);

        Receipt receipt = new Receipt(cashier1, items, 20.0);
        analytics.addReceipt(receipt);

        assertEquals(1, analytics.getTotalTransactions());
        assertEquals(20.0, analytics.getTotalRevenue());
    }

    @Test
    void testAddReceiptWithNullReceipt() {
        assertThrows(IllegalArgumentException.class, () -> analytics.addReceipt(null));
    }

    @Test
    void testAddExpense() {
        analytics.addExpense(1000.0);
        assertEquals(5200.0, analytics.getTotalExpenses());
    }

    @Test
    void testAddExpenseWithNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> analytics.addExpense(-1000.0));
    }

    @Test
    void testGetProfit() {
        Map<Product, Integer> items = new HashMap<>();
        items.put(milk, 2);
        items.put(bread, 3);

        Receipt receipt = new Receipt(cashier1, items, 20.0);
        analytics.addReceipt(receipt);

        assertEquals(20.0, analytics.getTotalRevenue());
        assertEquals(4200.0, analytics.getTotalExpenses());
        assertEquals(-4180.0, analytics.getProfit());
    }

    @Test
    void testGetTopSellingProducts() {
        // Create multiple receipts with different products
        Map<Product, Integer> items1 = new HashMap<>();
        items1.put(milk, 2);
        items1.put(bread, 3);
        Receipt receipt1 = new Receipt(cashier1, items1, 20.0);
        analytics.addReceipt(receipt1);

        Map<Product, Integer> items2 = new HashMap<>();
        items2.put(soap, 5);
        items2.put(paper, 2);
        Receipt receipt2 = new Receipt(cashier2, items2, 30.0);
        analytics.addReceipt(receipt2);

        Map<Product, Integer> items3 = new HashMap<>();
        items3.put(milk, 3);
        items3.put(soap, 2);
        Receipt receipt3 = new Receipt(cashier1, items3, 25.0);
        analytics.addReceipt(receipt3);

        Map<Product, Integer> topProducts = analytics.getTopSellingProducts(4);
        
        assertEquals(4, topProducts.size());
        assertEquals(5, topProducts.get(milk));
        assertEquals(7, topProducts.get(soap));
        assertEquals(3, topProducts.get(bread));
        assertEquals(2, topProducts.get(paper));
    }

    @Test
    void testGetTopSellingProductsWithNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> analytics.getTopSellingProducts(-1));
    }

    @Test
    void testGetTopSellingProductsWithZeroLimit() {
        Map<Product, Integer> items = new HashMap<>();
        items.put(milk, 2);
        Receipt receipt = new Receipt(cashier1, items, 20.0);
        analytics.addReceipt(receipt);

        Map<Product, Integer> topProducts = analytics.getTopSellingProducts(0);
        assertTrue(topProducts.isEmpty());
    }

    @Test
    void testGetTopPerformingCashiers() {
        // Create multiple receipts for different cashiers
        Map<Product, Integer> items1 = new HashMap<>();
        items1.put(milk, 2);
        items1.put(bread, 3);
        Receipt receipt1 = new Receipt(cashier1, items1, 20.0);
        analytics.addReceipt(receipt1);

        Map<Product, Integer> items2 = new HashMap<>();
        items2.put(soap, 5);
        items2.put(paper, 2);
        Receipt receipt2 = new Receipt(cashier2, items2, 30.0);
        analytics.addReceipt(receipt2);

        Map<Product, Integer> items3 = new HashMap<>();
        items3.put(milk, 3);
        items3.put(soap, 2);
        Receipt receipt3 = new Receipt(cashier1, items3, 25.0);
        analytics.addReceipt(receipt3);

        Map<Cashier, Double> topCashiers = analytics.getTopPerformingCashiers(2);
        
        assertEquals(2, topCashiers.size());
        assertEquals(45.0, topCashiers.get(cashier1));
        assertEquals(30.0, topCashiers.get(cashier2));
    }

    @Test
    void testGetTopPerformingCashiersWithNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> analytics.getTopPerformingCashiers(-1));
    }

    @Test
    void testGetTopPerformingCashiersWithZeroLimit() {
        Map<Product, Integer> items = new HashMap<>();
        items.put(milk, 2);
        Receipt receipt = new Receipt(cashier1, items, 20.0);
        analytics.addReceipt(receipt);

        Map<Cashier, Double> topCashiers = analytics.getTopPerformingCashiers(0);
        assertTrue(topCashiers.isEmpty());
    }

    @Test
    void testConcurrentOperations() throws InterruptedException {
        int numThreads = 10;
        int numOperations = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < numOperations; j++) {
                        Map<Product, Integer> items = new HashMap<>();
                        items.put(milk, 1);
                        Receipt receipt = new Receipt(cashier1, items, 10.0);
                        analytics.addReceipt(receipt);
                        analytics.addExpense(5.0);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(numThreads * numOperations, analytics.getTotalTransactions());
        assertEquals(numThreads * numOperations * 10.0, analytics.getTotalRevenue());
        assertEquals(4200.0 + (numThreads * numOperations * 5.0), analytics.getTotalExpenses());
    }

    @Test
    void testGenerateReport() {
        // Add some receipts
        Map<Product, Integer> items1 = new HashMap<>();
        items1.put(milk, 2);
        items1.put(bread, 3);
        Receipt receipt1 = new Receipt(cashier1, items1, 20.0);
        analytics.addReceipt(receipt1);

        Map<Product, Integer> items2 = new HashMap<>();
        items2.put(soap, 5);
        items2.put(paper, 2);
        Receipt receipt2 = new Receipt(cashier2, items2, 30.0);
        analytics.addReceipt(receipt2);

        String report = analytics.generateReport();
        
        assertNotNull(report);
        assertTrue(report.contains("Store Analytics Report"));
        assertTrue(report.contains("Financial Summary"));
        assertTrue(report.contains("Sales Performance"));
        assertTrue(report.contains("Top Selling Products"));
        assertTrue(report.contains("Top Performing Cashiers"));
        assertTrue(report.contains("Total Revenue: $50.00"));
        assertTrue(report.contains("Total Expenses: $4,200.00"));
        assertTrue(report.contains("Net Profit: $-4,150.00"));
    }

    @Test
    void testGenerateReportWithNoSales() {
        String report = analytics.generateReport();
        
        assertNotNull(report);
        assertTrue(report.contains("Store Analytics Report"));
        assertTrue(report.contains("Financial Summary"));
        assertTrue(report.contains("Total Revenue: $0.00"));
        assertTrue(report.contains("Total Expenses: $4,200.00"));
        assertTrue(report.contains("Net Profit: $-4,200.00"));
    }

    @Test
    void testGetAverageTransactionValue() {
        Map<Product, Integer> items1 = new HashMap<>();
        items1.put(milk, 2);
        items1.put(bread, 3);
        Receipt receipt1 = new Receipt(cashier1, items1, 20.0);
        analytics.addReceipt(receipt1);

        Map<Product, Integer> items2 = new HashMap<>();
        items2.put(soap, 5);
        items2.put(paper, 2);
        Receipt receipt2 = new Receipt(cashier2, items2, 30.0);
        analytics.addReceipt(receipt2);

        assertEquals(25.0, analytics.getAverageTransactionValue());
    }

    @Test
    void testGetAverageTransactionValueWithNoSales() {
        assertEquals(0.0, analytics.getAverageTransactionValue());
    }

    @Test
    void testGetTotalRevenue() {
        Map<Product, Integer> items = new HashMap<>();
        items.put(milk, 2);
        Receipt receipt = new Receipt(cashier1, items, 20.0);
        analytics.addReceipt(receipt);

        assertEquals(20.0, analytics.getTotalRevenue());
    }

    @Test
    void testGetTotalRevenueWithNoSales() {
        assertEquals(0.0, analytics.getTotalRevenue());
    }
} 