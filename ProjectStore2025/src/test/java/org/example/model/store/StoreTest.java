package org.example.model.store;

import org.example.model.product.FoodProduct;
import org.example.model.product.NonFoodProduct;
import org.example.model.product.Product;
import org.example.model.product.ProductCategory;
import org.example.exception.StoreException;
import org.example.exception.ReceiptException;
import org.example.config.StoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class StoreTest {
    @TempDir
    File tempDir;
    
    private Store store;
    private FoodProduct milk;
    private FoodProduct bread;
    private NonFoodProduct soap;
    private NonFoodProduct paper;
    private Cashier cashier1;
    private Cashier cashier2;
    private CashRegister register1;
    private CashRegister register2;

    @BeforeEach
    void setUp() {
        // Set the receipts directory to the temp directory for testing
        System.setProperty("receipts.directory", tempDir.getAbsolutePath());
        
        store = new Store("Test Store", 0.15, 0.20, 7, 0.20);
        cashier1 = new Cashier("John Doe", 2000.0);
        cashier2 = new Cashier("Jane Smith", 2200.0);
        register1 = new CashRegister(1);
        register2 = new CashRegister(2);

        milk = new FoodProduct("Milk", 2.0, 2.5, LocalDate.now().plusDays(7));
        bread = new FoodProduct("Bread", 1.5, 2.0, LocalDate.now().plusDays(5));
        soap = new NonFoodProduct("Soap", 1.0, 1.5);
        paper = new NonFoodProduct("Paper", 3.0, 4.0);

        store.addCashier(cashier1);
        store.addCashier(cashier2);
        store.addRegister(register1);
        store.addRegister(register2);
        register1.setAssignedCashier(cashier1);
        register2.setAssignedCashier(cashier2);

        store.addProduct(milk, 100, 20, 50);
        store.addProduct(bread, 50, 10, 30);
        store.addProduct(soap, 200, 30, 100);
        store.addProduct(paper, 150, 25, 75);
    }

    @Test
    void testStoreCreation() {
        assertEquals("Test Store", store.getName());
        assertEquals(0, store.getTotalRevenue());
        assertEquals(0, store.getTotalExpenses());
    }

    @Test
    void testAddAndRemoveCashier() {
        Cashier newCashier = new Cashier("New Cashier", 1800.0);
        store.addCashier(newCashier);
        assertTrue(store.getCashiers().contains(newCashier));
        assertEquals(3, store.getCashiers().size());

        store.removeCashier(newCashier);
        assertFalse(store.getCashiers().contains(newCashier));
        assertEquals(2, store.getCashiers().size());
    }

    @Test
    void testAddAndRemoveRegister() {
        CashRegister newRegister = new CashRegister(3);
        store.addRegister(newRegister);
        assertTrue(store.getRegisters().contains(newRegister));
        assertEquals(3, store.getRegisters().size());

        store.removeRegister(newRegister);
        assertFalse(store.getRegisters().contains(newRegister));
        assertEquals(2, store.getRegisters().size());
    }

    @Test
    void testAddAndRemoveProduct() {
        NonFoodProduct newProduct = new NonFoodProduct("New Product", 1.0, 1.5);
        store.addProduct(newProduct, 50, 10, 20);
        assertTrue(store.getProducts().contains(newProduct));
        assertEquals(3, store.getProducts().size());

        store.removeProduct(newProduct);
        assertFalse(store.getProducts().contains(newProduct));
        assertEquals(2, store.getProducts().size());
    }

    @Test
    void testProcessSale() {
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(soap, 3);

        double expectedTotal = (milk.calculateSellingPrice(0.15, 7, 0.20) * 2) +
                             (soap.calculateSellingPrice(0.15, 7, 0.20) * 3);

        assertDoesNotThrow(() -> {
            store.processSale(register1, sale, expectedTotal);
        });

        assertEquals(expectedTotal, store.getTotalRevenue());
    }

    @Test
    void testProcessSaleWithExpiredProduct() {
        FoodProduct expiredMilk = new FoodProduct("Expired Milk", 2.0, 2.5, LocalDate.now().minusDays(1));
        store.addProduct(expiredMilk, 10, 5, 20);

        Map<Product, Integer> sale = new HashMap<>();
        sale.put(expiredMilk, 1);

        assertThrows(StoreException.class, () -> {
            store.processSale(register1, sale, 10.0);
        });
    }

    @Test
    void testProcessSaleWithInsufficientPayment() {
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(soap, 3);

        double expectedTotal = (milk.calculateSellingPrice(0.15, 7, 0.20) * 2) +
                             (soap.calculateSellingPrice(0.15, 7, 0.20) * 3);

        assertThrows(StoreException.class, () -> {
            store.processSale(register1, sale, expectedTotal - 1);
        });
    }

    @Test
    void testProcessSaleWithInsufficientStock() {
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 200); // Only 100 in stock

        assertThrows(StoreException.class, () -> {
            store.processSale(register1, sale, 1000.0);
        });
    }

    @Test
    void testProcessSaleWithUnassignedRegister() {
        CashRegister unassignedRegister = new CashRegister(3);
        store.addRegister(unassignedRegister);

        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);

        assertThrows(StoreException.class, () -> {
            store.processSale(unassignedRegister, sale, 10.0);
        });
    }

    @Test
    void testFinancialCalculations() {
        // Initial expenses from cashiers
        double expectedExpenses = cashier1.getMonthlySalary() + cashier2.getMonthlySalary();
        assertEquals(expectedExpenses, store.getTotalExpenses());

        // Process a sale
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(soap, 3);

        double expectedRevenue = (milk.calculateSellingPrice(0.15, 7, 0.20) * 2) +
                               (soap.calculateSellingPrice(0.15, 7, 0.20) * 3);

        store.processSale(register1, sale, expectedRevenue);

        assertEquals(expectedRevenue, store.getTotalRevenue());
        assertEquals(expectedRevenue - expectedExpenses, store.getProfit());
    }

    @Test
    void testReceiptSaving() {
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(soap, 3);

        double expectedTotal = (milk.calculateSellingPrice(0.15, 7, 0.20) * 2) +
                             (soap.calculateSellingPrice(0.15, 7, 0.20) * 3);

        assertDoesNotThrow(() -> {
            store.processSale(register1, sale, expectedTotal);
        });

        // Verify receipt was created
        assertEquals(1, store.getReceipts().size());
    }

    @Test
    void testReceiptSavingWithInvalidDirectory() {
        // Set an invalid directory
        System.setProperty("user.home", tempDir.getAbsolutePath());
        
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(soap, 3);

        double expectedTotal = (milk.calculateSellingPrice(0.15, 7, 0.20) * 2) +
                             (soap.calculateSellingPrice(0.15, 7, 0.20) * 3);

        assertThrows(ReceiptException.class, () -> {
            store.processSale(register1, sale, expectedTotal);
        });
    }

    @Test
    void testAnalyticsReport() {
        // Process multiple sales
        Map<Product, Integer> sale1 = new HashMap<>();
        sale1.put(milk, 2);
        sale1.put(soap, 3);
        store.processSale(register1, sale1, 20.0);

        Map<Product, Integer> sale2 = new HashMap<>();
        sale2.put(soap, 5);
        store.processSale(register2, sale2, 30.0);

        String report = store.getAnalyticsReport();
        assertNotNull(report);
        assertTrue(report.contains("Store Analytics Report"));
        assertTrue(report.contains("Financial Summary"));
        assertTrue(report.contains("Sales Performance"));
        assertTrue(report.contains("Top Selling Products"));
        assertTrue(report.contains("Top Performing Cashiers"));
    }

    @Test
    void testInventoryReport() {
        String report = store.getInventoryReport();
        assertNotNull(report);
        assertTrue(report.contains("Inventory Report"));
        assertTrue(report.contains("Current Stock Levels"));
        assertTrue(report.contains(milk.getName()));
        assertTrue(report.contains(soap.getName()));
    }

    @Test
    void testLowStockAlert() {
        // Process a sale that brings stock below reorder point
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 85); // Initial stock is 100, reorder point is 20
        store.processSale(register1, sale, 200.0);

        String report = store.getInventoryReport();
        assertTrue(report.contains("LOW STOCK"));
        assertTrue(report.contains("Reorder Point"));
    }

    @Test
    void testExpiredProductAlert() {
        // Add a product that's about to expire
        FoodProduct expiringMilk = new FoodProduct("Expiring Milk", 2.0, 2.5, LocalDate.now().plusDays(1));
        store.addProduct(expiringMilk, 10, 5, 20);

        String report = store.getInventoryReport();
        assertTrue(report.contains("Expiring Milk"));
    }
} 