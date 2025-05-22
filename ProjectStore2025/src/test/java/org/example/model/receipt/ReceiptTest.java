package org.example.model.receipt;

import org.example.model.product.FoodProduct;
import org.example.model.product.NonFoodProduct;
import org.example.model.product.Product;
import org.example.model.store.Cashier;
import org.example.exception.ReceiptException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiptTest {
    private Cashier cashier;
    private FoodProduct milk;
    private NonFoodProduct soap;
    private Map<Product, Integer> items;

    @BeforeEach
    void setUp() {
        cashier = new Cashier("John Doe", 2000.0);
        milk = new FoodProduct("Milk", 2.0, 2.5, LocalDate.now().plusDays(7));
        soap = new NonFoodProduct("Soap", 1.0, 1.5);
        
        items = new HashMap<>();
        items.put(milk, 2);
        items.put(soap, 3);
    }

    @Test
    void testCreateReceipt() {
        Receipt receipt = new Receipt(cashier, items, 20.0);
        
        assertEquals(1, receipt.getReceiptNumber());
        assertEquals(cashier, receipt.getCashier());
        assertNotNull(receipt.getDateTime());
        assertEquals(items, receipt.getItems());
        assertEquals(20.0, receipt.getTotalAmount());
    }

    @Test
    void testCreateReceiptWithNullCashier() {
        assertThrows(ReceiptException.class, () -> 
            new Receipt(null, items, 20.0));
    }

    @Test
    void testCreateReceiptWithNullItems() {
        assertThrows(ReceiptException.class, () -> 
            new Receipt(cashier, null, 20.0));
    }

    @Test
    void testCreateReceiptWithEmptyItems() {
        assertThrows(ReceiptException.class, () -> 
            new Receipt(cashier, new HashMap<>(), 20.0));
    }

    @Test
    void testCreateReceiptWithNegativeAmount() {
        assertThrows(ReceiptException.class, () -> 
            new Receipt(cashier, items, -20.0));
    }

    @Test
    void testReceiptNumberIncrement() {
        Receipt receipt1 = new Receipt(cashier, items, 20.0);
        Receipt receipt2 = new Receipt(cashier, items, 20.0);
        Receipt receipt3 = new Receipt(cashier, items, 20.0);
        
        assertEquals(1, receipt1.getReceiptNumber());
        assertEquals(2, receipt2.getReceiptNumber());
        assertEquals(3, receipt3.getReceiptNumber());
    }

    @Test
    void testGetItemsReturnsCopy() {
        Receipt receipt = new Receipt(cashier, items, 20.0);
        Map<Product, Integer> returnedItems = receipt.getItems();
        
        // Modify the returned map
        returnedItems.put(milk, 5);
        
        // Original items should not be affected
        assertEquals(2, items.get(milk));
        assertEquals(2, receipt.getItems().get(milk));
    }

    @Test
    void testToString() {
        Receipt receipt = new Receipt(cashier, items, 20.0);
        String receiptString = receipt.toString();
        
        assertTrue(receiptString.contains("Receipt #1"));
        assertTrue(receiptString.contains("Cashier: John Doe"));
        assertTrue(receiptString.contains("Milk"));
        assertTrue(receiptString.contains("Soap"));
        assertTrue(receiptString.contains("Total Amount: 20.00"));
    }

    @Test
    void testToStringWithMultipleItems() {
        Map<Product, Integer> multipleItems = new HashMap<>();
        multipleItems.put(milk, 2);
        multipleItems.put(soap, 3);
        multipleItems.put(new FoodProduct("Bread", 1.5, 2.0, LocalDate.now().plusDays(5)), 4);
        
        Receipt receipt = new Receipt(cashier, multipleItems, 30.0);
        String receiptString = receipt.toString();
        
        assertTrue(receiptString.contains("Milk"));
        assertTrue(receiptString.contains("Soap"));
        assertTrue(receiptString.contains("Bread"));
        assertTrue(receiptString.contains("Total Amount: 30.00"));
    }

    @Test
    void testToStringWithZeroItems() {
        Map<Product, Integer> emptyItems = new HashMap<>();
        emptyItems.put(milk, 0);
        
        assertThrows(ReceiptException.class, () -> 
            new Receipt(cashier, emptyItems, 0.0));
    }

    @Test
    void testToStringWithLargeAmount() {
        Map<Product, Integer> largeItems = new HashMap<>();
        largeItems.put(milk, 100);
        largeItems.put(soap, 50);
        
        Receipt receipt = new Receipt(cashier, largeItems, 1000.0);
        String receiptString = receipt.toString();
        
        assertTrue(receiptString.contains("Total Amount: 1000.00"));
    }
} 