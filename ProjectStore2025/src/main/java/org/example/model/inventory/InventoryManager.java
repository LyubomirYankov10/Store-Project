package org.example.model.inventory;

import org.example.model.product.Product;
import org.example.model.product.FoodProduct;
import org.example.exception.ProductException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.NumberFormat;
import java.util.Locale;

public class InventoryManager {
    private final Map<Product, AtomicInteger> stockLevels;
    private final Map<Product, Integer> reorderPoints;
    private final Map<Product, Integer> reorderQuantities;
    private final List<Product> lowStockProducts;
    private final List<Product> expiredProducts;
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

    public InventoryManager() {
        this.stockLevels = new ConcurrentHashMap<>();
        this.reorderPoints = new ConcurrentHashMap<>();
        this.reorderQuantities = new ConcurrentHashMap<>();
        this.lowStockProducts = new CopyOnWriteArrayList<>();
        this.expiredProducts = new CopyOnWriteArrayList<>();
    }

    public void addProduct(Product product, int initialStock, int reorderPoint, int reorderQuantity) {
        if (product == null) {
            throw new ProductException("Product cannot be null");
        }
        if (initialStock < 0) {
            throw new ProductException("Initial stock cannot be negative");
        }
        if (reorderPoint < 0) {
            throw new ProductException("Reorder point cannot be negative");
        }
        if (reorderQuantity <= 0) {
            throw new ProductException("Reorder quantity must be positive");
        }

        stockLevels.put(product, new AtomicInteger(initialStock));
        reorderPoints.put(product, reorderPoint);
        reorderQuantities.put(product, reorderQuantity);
        checkStockLevel(product);
    }

    public void updateStock(Product product, int quantity) {
        if (product == null) {
            throw new ProductException("Product cannot be null");
        }

        AtomicInteger currentStock = stockLevels.get(product);
        if (currentStock == null) {
            throw new ProductException("Product not found in inventory");
        }

        int newStock = currentStock.addAndGet(quantity);
        if (newStock < 0) {
            currentStock.addAndGet(-quantity);
            throw new ProductException("Insufficient stock for product: " + product.getName());
        }

        checkStockLevel(product);
    }

    public int getStockLevel(Product product) {
        if (product == null) {
            throw new ProductException("Product cannot be null");
        }

        AtomicInteger stock = stockLevels.get(product);
        if (stock == null) {
            throw new ProductException("Product not found in inventory");
        }

        return stock.get();
    }

    public boolean needsReorder(Product product) {
        if (product == null) {
            throw new ProductException("Product cannot be null");
        }

        AtomicInteger stock = stockLevels.get(product);
        Integer reorderPoint = reorderPoints.get(product);
        if (stock == null || reorderPoint == null) {
            throw new ProductException("Product not found in inventory");
        }

        return stock.get() <= reorderPoint;
    }

    public int getReorderQuantity(Product product) {
        if (product == null) {
            throw new ProductException("Product cannot be null");
        }

        Integer quantity = reorderQuantities.get(product);
        if (quantity == null) {
            throw new ProductException("Product not found in inventory");
        }

        return quantity;
    }

    public List<Product> getLowStockProducts() {
        return Collections.unmodifiableList(lowStockProducts);
    }

    public List<Product> getExpiredProducts() {
        return Collections.unmodifiableList(expiredProducts);
    }

    private void checkStockLevel(Product product) {
        int currentStock = getStockLevel(product);
        int reorderPoint = reorderPoints.get(product);

        if (currentStock <= reorderPoint) {
            if (!lowStockProducts.contains(product)) {
                lowStockProducts.add(product);
            }
        } else {
            lowStockProducts.remove(product);
        }

        if (product.isExpired()) {
            if (!expiredProducts.contains(product)) {
                expiredProducts.add(product);
            }
        } else {
            expiredProducts.remove(product);
        }
    }

    public String generateInventoryReport() {
        StringBuilder report = new StringBuilder();
        report.append("Inventory Report:\n");
        report.append("----------------\n");

        for (Map.Entry<Product, AtomicInteger> entry : stockLevels.entrySet()) {
            Product product = entry.getKey();
            int stock = entry.getValue().get();
            int reorderPoint = reorderPoints.get(product);
            int reorderQuantity = reorderQuantities.get(product);

            report.append(String.format("%s:\n", product.getName()));
            report.append(String.format("  Current Stock: %d\n", stock));
            report.append(String.format("  Reorder Point: %d\n", reorderPoint));
            report.append(String.format("  Reorder Quantity: %d\n", reorderQuantity));
            report.append(String.format("  Status: %s\n", 
                stock <= reorderPoint ? "Needs Reorder" : "OK"));
            report.append("\n");
        }

        if (!lowStockProducts.isEmpty()) {
            report.append("Low Stock Products:\n");
            report.append("------------------\n");
            lowStockProducts.forEach(product ->
                report.append(String.format("- %s: %d units (Reorder Point: %d)\n",
                    product.getName(),
                    getStockLevel(product),
                    reorderPoints.get(product))));
            report.append("\n");
        }

        if (!expiredProducts.isEmpty()) {
            report.append("Expired Products:\n");
            report.append("----------------\n");
            expiredProducts.forEach(product -> {
                report.append(String.format("- %s: %d units", product.getName(), getStockLevel(product)));
                if (product instanceof FoodProduct) {
                    report.append(" (Expired on: ").append(((FoodProduct) product).getExpirationDate()).append(")");
                }
                report.append("\n");
            });
        }

        return report.toString();
    }

    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("Inventory Report\n");
        report.append("================\n\n");
        
        report.append("Current Stock Levels:\n");
        report.append("--------------------\n");
        
        for (Map.Entry<Product, AtomicInteger> entry : stockLevels.entrySet()) {
            Product product = entry.getKey();
            int stock = entry.getValue().get();
            int reorderPoint = reorderPoints.get(product);
            int reorderQuantity = reorderQuantities.get(product);
            
            report.append(String.format("%s:\n", product.getName()));
            report.append(String.format("  Current Stock: %d units\n", stock));
            report.append(String.format("  Reorder Point: %d units\n", reorderPoint));
            report.append(String.format("  Reorder Quantity: %d units\n", reorderQuantity));
            
            if (stock <= reorderPoint) {
                report.append("  STATUS: LOW STOCK - Reorder needed!\n");
            }
            
            if (product.isNearExpiration(7)) {
                report.append("  STATUS: NEAR EXPIRATION - Consider discounting!\n");
            }
            
            report.append("\n");
        }
        
        return report.toString();
    }
} 