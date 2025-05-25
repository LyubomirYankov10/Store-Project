package org.example.model.product;

import org.example.exception.ProductException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {
    private FoodProduct foodProduct;
    private NonFoodProduct nonFoodProduct;
    private LocalDate futureDate;
    private LocalDate pastDate;

    @BeforeEach
    void setUp() {
        futureDate = LocalDate.now().plusDays(10);
        pastDate = LocalDate.now().minusDays(1);
        foodProduct = new FoodProduct("F001", "Milk", 2.50, futureDate, 10);
        nonFoodProduct = new NonFoodProduct("NF001", "Soap", 1.50, futureDate, 20);
    }

    @Test
    void testProductCreation() {
        assertEquals("F001", foodProduct.getId());
        assertEquals("Milk", foodProduct.getName());
        assertEquals(2.50, foodProduct.getDeliveryPrice());
        assertEquals(ProductCategory.FOOD, foodProduct.getCategory());
        assertEquals(10, foodProduct.getQuantity());
    }

    @Test
    void testExpiredProduct() {
        FoodProduct expiredProduct = new FoodProduct("F002", "Expired Milk", 2.50, pastDate, 5);
        assertTrue(expiredProduct.isExpired());
        assertThrows(IllegalStateException.class, () -> 
            expiredProduct.calculateSellingPrice(0.20, 7, 0.10));
    }

    @Test
    void testNearExpiration() {
        LocalDate nearExpirationDate = LocalDate.now().plusDays(5);
        FoodProduct nearExpirationProduct = new FoodProduct("F003", "Near Expiration Milk", 
            2.50, nearExpirationDate, 5);
        assertTrue(nearExpirationProduct.isNearExpiration(7));
    }

    @Test
    void testQuantityManagement() {
        foodProduct.reduceQuantity(5);
        assertEquals(5, foodProduct.getQuantity());

        foodProduct.addQuantity(3);
        assertEquals(8, foodProduct.getQuantity());

        assertThrows(IllegalArgumentException.class, () -> foodProduct.reduceQuantity(10));
        assertThrows(IllegalArgumentException.class, () -> foodProduct.addQuantity(-1));
    }

    @Test
    void testPriceCalculation() {
        double foodPrice = foodProduct.calculateSellingPrice(0.20, 7, 0.10);
        assertEquals(3.00, foodPrice, 0.001);

        double nonFoodPrice = nonFoodProduct.calculateSellingPrice(0.15, 7, 0.10);
        assertEquals(1.725, nonFoodPrice, 0.001);
    }
} 