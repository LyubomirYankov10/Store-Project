package org.example.model.inventory;

import org.example.model.product.FoodProduct;
import org.example.model.product.NonFoodProduct;
import org.example.model.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InventoryManagerTest {
    private InventoryManager inventoryManager;
    private FoodProduct milk;
    private FoodProduct bread;
    private NonFoodProduct soap;
    private NonFoodProduct paper;

    @BeforeEach
    void setUp() {
        inventoryManager = new InventoryManager();
        
        milk = new FoodProduct("Milk", 2.0, 2.5, LocalDate.now().plusDays(7));
        bread = new FoodProduct("Bread", 1.5, 2.0, LocalDate.now().plusDays(5));
        soap = new NonFoodProduct("Soap", 1.0, 1.5);
        paper = new NonFoodProduct("Paper", 3.0, 4.0);

        inventoryManager.addProduct(milk, 100, 20, 50);
        inventoryManager.addProduct(bread, 50, 10, 30);
        inventoryManager.addProduct(soap, 200, 30, 100);
        inventoryManager.addProduct(paper, 150, 25, 75);
    }

    @Test
    void testAddProduct() {
        NonFoodProduct newProduct = new NonFoodProduct("New Product", 1.0, 1.5);
        inventoryManager.addProduct(newProduct, 50, 10, 20);
        
        assertEquals(50, inventoryManager.getStockLevel(newProduct));
        assertTrue(inventoryManager.getLowStockProducts().isEmpty());
    }

    @Test
    void testAddProductWithNullProduct() {
        assertThrows(IllegalArgumentException.class, () -> 
            inventoryManager.addProduct(null, 50, 10, 20));
    }

    @Test
    void testAddProductWithNegativeStock() {
        NonFoodProduct newProduct = new NonFoodProduct("New Product", 1.0, 1.5);
        assertThrows(IllegalArgumentException.class, () -> 
            inventoryManager.addProduct(newProduct, -50, 10, 20));
    }

    @Test
    void testAddProductWithNegativeReorderPoint() {
        NonFoodProduct newProduct = new NonFoodProduct("New Product", 1.0, 1.5);
        assertThrows(IllegalArgumentException.class, () -> 
            inventoryManager.addProduct(newProduct, 50, -10, 20));
    }

    @Test
    void testAddProductWithNegativeReorderQuantity() {
        NonFoodProduct newProduct = new NonFoodProduct("New Product", 1.0, 1.5);
        assertThrows(IllegalArgumentException.class, () -> 
            inventoryManager.addProduct(newProduct, 50, 10, -20));
    }

    @Test
    void testUpdateStock() {
        inventoryManager.updateStock(milk, -30);
        assertEquals(70, inventoryManager.getStockLevel(milk));
        assertFalse(inventoryManager.needsReorder(milk));
    }

    @Test
    void testUpdateStockBelowReorderPoint() {
        inventoryManager.updateStock(milk, -85);
        assertEquals(15, inventoryManager.getStockLevel(milk));
        assertTrue(inventoryManager.needsReorder(milk));
        assertTrue(inventoryManager.getLowStockProducts().contains(milk));
    }

    @Test
    void testUpdateStockWithNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () -> 
            inventoryManager.updateStock(milk, -150));
    }

    @Test
    void testGetStockLevel() {
        assertEquals(100, inventoryManager.getStockLevel(milk));
        assertEquals(50, inventoryManager.getStockLevel(bread));
        assertEquals(200, inventoryManager.getStockLevel(soap));
        assertEquals(150, inventoryManager.getStockLevel(paper));
    }

    @Test
    void testGetStockLevelForNonExistentProduct() {
        NonFoodProduct newProduct = new NonFoodProduct("New Product", 1.0, 1.5);
        assertEquals(0, inventoryManager.getStockLevel(newProduct));
    }

    @Test
    void testNeedsReorder() {
        assertFalse(inventoryManager.needsReorder(milk));
        inventoryManager.updateStock(milk, -85);
        assertTrue(inventoryManager.needsReorder(milk));
    }

    @Test
    void testGetReorderQuantity() {
        assertEquals(50, inventoryManager.getReorderQuantity(milk));
        assertEquals(30, inventoryManager.getReorderQuantity(bread));
        assertEquals(100, inventoryManager.getReorderQuantity(soap));
        assertEquals(75, inventoryManager.getReorderQuantity(paper));
    }

    @Test
    void testGetLowStockProducts() {
        inventoryManager.updateStock(milk, -85);
        inventoryManager.updateStock(bread, -45);
        
        List<Product> lowStockProducts = inventoryManager.getLowStockProducts();
        assertEquals(2, lowStockProducts.size());
        assertTrue(lowStockProducts.contains(milk));
        assertTrue(lowStockProducts.contains(bread));
    }

    @Test
    void testGetExpiredProducts() {
        FoodProduct expiredMilk = new FoodProduct("Expired Milk", 2.0, 2.5, LocalDate.now().minusDays(1));
        inventoryManager.addProduct(expiredMilk, 10, 5, 20);
        
        List<Product> expiredProducts = inventoryManager.getExpiredProducts();
        assertEquals(1, expiredProducts.size());
        assertTrue(expiredProducts.contains(expiredMilk));
    }

    @Test
    void testGetExpiredProductsWithNearExpiration() {
        FoodProduct expiringMilk = new FoodProduct("Expiring Milk", 2.0, 2.5, LocalDate.now().plusDays(1));
        inventoryManager.addProduct(expiringMilk, 10, 5, 20);
        
        List<Product> expiredProducts = inventoryManager.getExpiredProducts();
        assertEquals(1, expiredProducts.size());
        assertTrue(expiredProducts.contains(expiringMilk));
    }

    @Test
    void testGenerateInventoryReport() {
        String report = inventoryManager.generateInventoryReport();
        
        assertNotNull(report);
        assertTrue(report.contains("Inventory Report"));
        assertTrue(report.contains("Current Stock Levels"));
        assertTrue(report.contains(milk.getName()));
        assertTrue(report.contains(bread.getName()));
        assertTrue(report.contains(soap.getName()));
        assertTrue(report.contains(paper.getName()));
    }

    @Test
    void testGenerateInventoryReportWithLowStock() {
        inventoryManager.updateStock(milk, -85);
        inventoryManager.updateStock(bread, -45);
        
        String report = inventoryManager.generateInventoryReport();
        
        assertTrue(report.contains("LOW STOCK"));
        assertTrue(report.contains("Reorder Point"));
        assertTrue(report.contains(milk.getName()));
        assertTrue(report.contains(bread.getName()));
    }

    @Test
    void testGenerateInventoryReportWithExpiredProducts() {
        FoodProduct expiredMilk = new FoodProduct("Expired Milk", 2.0, 2.5, LocalDate.now().minusDays(1));
        inventoryManager.addProduct(expiredMilk, 10, 5, 20);
        
        String report = inventoryManager.generateInventoryReport();
        
        assertTrue(report.contains("EXPIRED"));
        assertTrue(report.contains(expiredMilk.getName()));
    }

    @Test
    void testGenerateInventoryReportWithNearExpiration() {
        FoodProduct expiringMilk = new FoodProduct("Expiring Milk", 2.0, 2.5, LocalDate.now().plusDays(1));
        inventoryManager.addProduct(expiringMilk, 10, 5, 20);
        
        String report = inventoryManager.generateInventoryReport();
        
        assertTrue(report.contains("EXPIRING SOON"));
        assertTrue(report.contains(expiringMilk.getName()));
    }
} 