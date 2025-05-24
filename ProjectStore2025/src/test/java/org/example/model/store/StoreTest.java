package org.example.model.store;

import org.example.model.product.FoodProduct;
import org.example.model.product.NonFoodProduct;
import org.example.model.product.Product;
import org.example.model.receipt.Receipt;
import org.example.exception.StoreException;
import org.example.config.StoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class StoreTest {
    private Store store;
    private Cashier cashier1;
    private Cashier cashier2;
    private CashRegister register1;
    private CashRegister register2;
    private FoodProduct milk;
    private FoodProduct bread;
    private NonFoodProduct soap;
    private NonFoodProduct paper;

    @TempDir
    File tempDir;

    @BeforeEach
    void setUp() {
        System.setProperty("user.home", tempDir.getAbsolutePath());
        String receiptsDir = tempDir.getAbsolutePath() + File.separator + "store_receipts";
        System.setProperty("receipts.directory", receiptsDir);

        store = new Store(
            "Test Store",
            0.20,
            0.15,
            7,
            0.30
        );

        cashier1 = new Cashier("John Doe", 2000.0);
        cashier2 = new Cashier("Jane Smith", 2200.0);
        store.addCashier(cashier1);
        store.addCashier(cashier2);

        register1 = new CashRegister(1);
        register2 = new CashRegister(2);
        store.addRegister(register1);
        store.addRegister(register2);

        register1.setAssignedCashier(cashier1);
        register2.setAssignedCashier(cashier2);

        milk = new FoodProduct("Milk", 2.0, 2.5, LocalDate.now().plusDays(7));
        bread = new FoodProduct("Bread", 1.5, 2.0, LocalDate.now().plusDays(5));
        soap = new NonFoodProduct("Soap", 1.0, 1.5);
        paper = new NonFoodProduct("Paper", 3.0, 4.0);

        store.addProduct(milk, 100, 20, 50);
        store.addProduct(bread, 50, 10, 30);
        store.addProduct(soap, 200, 30, 100);
        store.addProduct(paper, 150, 25, 75);
    }

    @Test
    void testProcessSale() {
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(bread, 3);

        Receipt receipt = store.processSale(register1, sale, 20.0);

        assertNotNull(receipt);
        assertEquals(2, receipt.getItems().get(milk));
        assertEquals(3, receipt.getItems().get(bread));
        assertEquals(98, milk.getQuantity());
        assertEquals(47, bread.getQuantity());
    }

    @Test
    void testProcessSaleWithInsufficientStock() {
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 200);

        assertThrows(StoreException.class, () -> {
            store.processSale(register1, sale, 500.0);
        });

        assertEquals(100, milk.getQuantity());
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
        sale.put(bread, 3);

        assertThrows(StoreException.class, () -> {
            store.processSale(register1, sale, 1.0);
        });
    }

    @Test
    void testStoreFinancials() {
        double initialExpenses = 4200.0;

        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(bread, 3);
        store.processSale(register1, sale, 20.0);

        assertEquals(20.0, store.getTotalRevenue(), 0.001);
        assertEquals(initialExpenses, store.getTotalExpenses(), 0.001);
        assertEquals(20.0 - initialExpenses, store.getProfit(), 0.001);
    }

    @Test
    void testReceiptGeneration() {
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(bread, 3);

        Receipt receipt = store.processSale(register1, sale, 20.0);

        assertNotNull(receipt);
        assertEquals(cashier1, receipt.getCashier());
        assertEquals(2, receipt.getItems().get(milk));
        assertEquals(3, receipt.getItems().get(bread));
    }

    @Test
    void testInvalidReceiptsDirectory() {
        System.setProperty("receipts.directory", "/invalid/path");

        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(bread, 3);

        assertThrows(StoreException.class, () -> {
            store.processSale(register1, sale, 20.0);
        });
    }

    @Test
    void testMultipleSales() {
        Map<Product, Integer> sale1 = new HashMap<>();
        sale1.put(milk, 2);
        sale1.put(bread, 3);
        store.processSale(register1, sale1, 20.0);

        Map<Product, Integer> sale2 = new HashMap<>();
        sale2.put(soap, 5);
        sale2.put(paper, 2);
        store.processSale(register2, sale2, 30.0);

        assertEquals(50.0, store.getTotalRevenue(), 0.001);
        assertEquals(96, milk.getQuantity());
        assertEquals(47, bread.getQuantity());
        assertEquals(195, soap.getQuantity());
        assertEquals(148, paper.getQuantity());
    }

    @Test
    void testLowStockAlert() {
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 85);

        store.processSale(register1, sale, 200.0);

        String inventoryReport = store.getInventoryReport();
        assertTrue(inventoryReport.contains("Needs Reorder"));
    }

    @Test
    void testNearExpirationDiscount() {
        FoodProduct nearExpirationMilk = new FoodProduct(
            "Near Expiration Milk",
            2.0,
            2.5,
            LocalDate.now().plusDays(3)
        );
        store.addProduct(nearExpirationMilk, 10, 5, 20);

        Map<Product, Integer> sale = new HashMap<>();
        sale.put(nearExpirationMilk, 1);

        Receipt receipt = store.processSale(register1, sale, 10.0);
        double expectedPrice = 2.5 * 1.2 * 0.7;
        assertEquals(expectedPrice, receipt.getTotalAmount(), 0.001);
    }
} 