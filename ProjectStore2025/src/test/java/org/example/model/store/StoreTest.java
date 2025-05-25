package org.example.model.store;

import org.example.model.product.FoodProduct;
import org.example.model.product.NonFoodProduct;
import org.example.model.product.Product;
import org.example.model.product.ProductCategory;
import org.example.model.receipt.Receipt;
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
        sale.put(milk, 200);

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
    void testPriceCalculations() {
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(bread, 3);
        sale.put(soap, 5);
        sale.put(paper, 2);

        double milkPrice = milk.calculateSellingPrice(0.20, 7, 0.10);
        double breadPrice = bread.calculateSellingPrice(0.20, 7, 0.10);
        double soapPrice = soap.calculateSellingPrice(0.15, 7, 0.10);
        double paperPrice = paper.calculateSellingPrice(0.15, 7, 0.10);

        double expectedTotal = (milkPrice * 2) + (breadPrice * 3) + (soapPrice * 5) + (paperPrice * 2);
        expectedTotal = Math.round(expectedTotal * 100.0) / 100.0;

        Receipt receipt = store.processSale(register1, sale, expectedTotal);
        assertEquals(expectedTotal, receipt.getTotalAmount(), 0.001);
    }

    @Test
    void testFinancialCalculations() {
        double expectedExpenses = cashier1.getMonthlySalary() + cashier2.getMonthlySalary();
        assertEquals(expectedExpenses, store.getTotalExpenses());

        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 2);
        sale.put(soap, 3);

        double milkPrice = milk.calculateSellingPrice(0.20, 7, 0.10);
        double soapPrice = soap.calculateSellingPrice(0.15, 7, 0.10);
        double expectedRevenue = (milkPrice * 2) + (soapPrice * 3);
        expectedRevenue = Math.round(expectedRevenue * 100.0) / 100.0;

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

        assertEquals(1, store.getReceipts().size());
    }

    @Test
    void testReceiptSavingWithInvalidDirectory() {
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
        Map<Product, Integer> sale = new HashMap<>();
        sale.put(milk, 85);
        store.processSale(register1, sale, 200.0);

        String report = store.getInventoryReport();
        assertTrue(report.contains("LOW STOCK"));
        assertTrue(report.contains("Reorder Point"));
    }

    @Test
    void testExpiredProductAlert() {
        FoodProduct expiringMilk = new FoodProduct("Expiring Milk", 2.0, 2.5, LocalDate.now().plusDays(1));
        store.addProduct(expiringMilk, 10, 5, 20);

        String report = store.getInventoryReport();
        assertTrue(report.contains("Expiring Milk"));
    }

    @Test
    void testCashierValidation() {
        assertThrows(IllegalArgumentException.class, () -> new Cashier(null, 2000.0));
        assertThrows(IllegalArgumentException.class, () -> new Cashier("", 2000.0));
        assertThrows(IllegalArgumentException.class, () -> new Cashier("John Doe", 0));
        assertThrows(IllegalArgumentException.class, () -> new Cashier("John Doe", -1000.0));
    }

    @Test
    void testCashierSalaryUpdate() {
        Cashier cashier = new Cashier("John Doe", 2000.0);
        assertEquals(2000.0, cashier.getMonthlySalary());
        
        cashier.setMonthlySalary(2500.0);
        assertEquals(2500.0, cashier.getMonthlySalary());
        
        assertThrows(IllegalArgumentException.class, () -> cashier.setMonthlySalary(0));
        assertThrows(IllegalArgumentException.class, () -> cashier.setMonthlySalary(-1000.0));
    }

    @Test
    void testCashierNameUpdate() {
        Cashier cashier = new Cashier("John Doe", 2000.0);
        assertEquals("John Doe", cashier.getName());
        
        cashier.setName("Jane Smith");
        assertEquals("Jane Smith", cashier.getName());
        
        assertThrows(IllegalArgumentException.class, () -> cashier.setName(null));
        assertThrows(IllegalArgumentException.class, () -> cashier.setName(""));
    }

    @Test
    void testCashierIdGeneration() {
        Cashier cashier1 = new Cashier("John Doe", 2000.0);
        Cashier cashier2 = new Cashier("Jane Smith", 2200.0);
        Cashier cashier3 = new Cashier("Bob Johnson", 1800.0);
        
        assertEquals(1, cashier1.getId());
        assertEquals(2, cashier2.getId());
        assertEquals(3, cashier3.getId());
    }

    @Test
    void testRegisterValidation() {
        assertThrows(IllegalArgumentException.class, () -> new CashRegister(0));
        assertThrows(IllegalArgumentException.class, () -> new CashRegister(-1));
    }

    @Test
    void testRegisterTransactionManagement() {
        CashRegister register = new CashRegister(1);
        register.setAssignedCashier(cashier1);
        
        register.addToTransaction(milk, 2);
        register.addToTransaction(bread, 3);
        
        double expectedTotal = (milk.getDeliveryPrice() * 2) + (bread.getDeliveryPrice() * 3);
        assertEquals(expectedTotal, register.getCurrentTransactionTotal());
        
        register.removeFromTransaction(milk, 1);
        expectedTotal = (milk.getDeliveryPrice() * 1) + (bread.getDeliveryPrice() * 3);
        assertEquals(expectedTotal, register.getCurrentTransactionTotal());
        
        register.removeFromTransaction(milk, 1);
        expectedTotal = bread.getDeliveryPrice() * 3;
        assertEquals(expectedTotal, register.getCurrentTransactionTotal());
        
        register.clearTransaction();
        assertEquals(0, register.getCurrentTransactionTotal());
        assertTrue(register.getCurrentTransaction().isEmpty());
    }

    @Test
    void testRegisterTransactionValidation() {
        CashRegister register = new CashRegister(1);
        register.setAssignedCashier(cashier1);
        
        assertThrows(IllegalArgumentException.class, () -> register.addToTransaction(null, 1));
        assertThrows(IllegalArgumentException.class, () -> register.addToTransaction(milk, 0));
        assertThrows(IllegalArgumentException.class, () -> register.addToTransaction(milk, -1));
        
        assertThrows(IllegalArgumentException.class, () -> register.removeFromTransaction(null, 1));
        assertThrows(IllegalArgumentException.class, () -> register.removeFromTransaction(milk, 0));
        assertThrows(IllegalArgumentException.class, () -> register.removeFromTransaction(milk, -1));
        
        register.addToTransaction(milk, 2);
        assertThrows(IllegalStateException.class, () -> register.removeFromTransaction(milk, 3));
    }

    @Test
    void testConcurrentCashierAssignment() {
        CashRegister register = new CashRegister(1);
        Cashier cashier1 = new Cashier("John Doe", 2000.0);
        Cashier cashier2 = new Cashier("Jane Smith", 2200.0);

        register.setAssignedCashier(cashier1);
        assertThrows(IllegalStateException.class, () -> register.setAssignedCashier(cashier2));
    }

    @Test
    void testConcurrentTransactionModification() {
        CashRegister register = new CashRegister(1);
        register.setAssignedCashier(cashier1);

        register.addToTransaction(milk, 2);
        register.addToTransaction(bread, 3);

        Map<Product, Integer> transaction = register.getCurrentTransaction();
        transaction.put(milk, 5);

        assertEquals(2, register.getCurrentTransaction().get(milk));
    }

    @Test
    void testCashierAssignmentRemoval() {
        CashRegister register = new CashRegister(1);
        register.setAssignedCashier(cashier1);
        assertTrue(register.isAssigned());
        assertEquals(cashier1, register.getAssignedCashier());

        register.removeAssignedCashier();
        assertFalse(register.isAssigned());
        assertNull(register.getAssignedCashier());
    }

    @Test
    void testTransactionTotalCalculation() {
        CashRegister register = new CashRegister(1);
        register.setAssignedCashier(cashier1);

        register.addToTransaction(milk, 2);
        register.addToTransaction(bread, 3);

        double expectedTotal = (milk.getDeliveryPrice() * 2) + (bread.getDeliveryPrice() * 3);
        assertEquals(expectedTotal, register.getCurrentTransactionTotal());

        register.removeFromTransaction(milk, 1);
        expectedTotal = (milk.getDeliveryPrice() * 1) + (bread.getDeliveryPrice() * 3);
        assertEquals(expectedTotal, register.getCurrentTransactionTotal());
    }

    @Test
    void testTransactionValidation() {
        CashRegister register = new CashRegister(1);
        register.setAssignedCashier(cashier1);

        assertThrows(IllegalArgumentException.class, () -> register.addToTransaction(null, 1));
        assertThrows(IllegalArgumentException.class, () -> register.addToTransaction(milk, 0));
        assertThrows(IllegalArgumentException.class, () -> register.addToTransaction(milk, -1));

        register.addToTransaction(milk, 2);
        assertThrows(IllegalArgumentException.class, () -> register.removeFromTransaction(null, 1));
        assertThrows(IllegalArgumentException.class, () -> register.removeFromTransaction(milk, 0));
        assertThrows(IllegalArgumentException.class, () -> register.removeFromTransaction(milk, -1));
        assertThrows(IllegalStateException.class, () -> register.removeFromTransaction(milk, 3));
    }

    @Test
    void testCashierIdUniqueness() {
        Cashier cashier1 = new Cashier("John Doe", 2000.0);
        Cashier cashier2 = new Cashier("Jane Smith", 2200.0);
        Cashier cashier3 = new Cashier("Bob Johnson", 1800.0);

        assertNotEquals(cashier1.getId(), cashier2.getId());
        assertNotEquals(cashier2.getId(), cashier3.getId());
        assertNotEquals(cashier1.getId(), cashier3.getId());
    }
} 