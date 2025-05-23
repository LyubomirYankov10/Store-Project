package org.example.model.inventory;

import org.example.model.product.Product;
import org.example.model.product.FoodProduct;
import org.example.exception.ProductException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class InventoryManager {
    private final Map<Product, AtomicInteger> stockLevels;
    private final Map<Product, Integer> reorderPoints;
    private final Map<Product, Integer> reorderQuantities;
    private final List<Product> lowStockProducts;
    private final List<Product> expiredProducts;

    public InventoryManager() {
        this.stockLevels = new ConcurrentHashMap<>();
        this.reorderPoints = new ConcurrentHashMap<>();
        this.reorderQuantities = new ConcurrentHashMap<>();
        this.lowStockProducts = new CopyOnWriteArrayList<>();
        this.expiredProducts = new CopyOnWriteArrayList<>();
    }

    public void addProduct(Product product, int initialStock, int reorderPoint, int reorderQuantity) {
        if (product == null) {
            throw new ProductException("Cannot add null product to inventory");
        }
        if (initialStock < 0 || reorderPoint < 0 || reorderQuantity <= 0) {
            throw new ProductException("Invalid stock or reorder parameters");
        }

        stockLevels.put(product, new AtomicInteger(initialStock));
        reorderPoints.put(product, reorderPoint);
        reorderQuantities.put(product, reorderQuantity);
        checkStockLevel(product);
    }

    public void updateStock(Product product, int quantity) {
        if (!stockLevels.containsKey(product)) {
            throw new ProductException("Product not found in inventory: " + product.getName());
        }

        AtomicInteger currentStock = stockLevels.get(product);
        int newStock = currentStock.addAndGet(quantity);
        
        if (newStock < 0) {
            // Rollback the change
            currentStock.addAndGet(-quantity);
            throw new ProductException("Insufficient stock for product: " + product.getName());
        }

        checkStockLevel(product);
    }

    public int getStockLevel(Product product) {
        if (!stockLevels.containsKey(product)) {
            throw new ProductException("Product not found in inventory: " + product.getName());
        }
        return stockLevels.get(product).get();
    }

    public boolean needsReorder(Product product) {
        return getStockLevel(product) <= reorderPoints.getOrDefault(product, 0);
    }

    public int getReorderQuantity(Product product) {
        return reorderQuantities.getOrDefault(product, 0);
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
        report.append("Inventory Report\n");
        report.append("================\n\n");
        
        report.append("Current Stock Levels:\n");
        report.append("--------------------\n");
        for (Map.Entry<Product, AtomicInteger> entry : stockLevels.entrySet()) {
            Product product = entry.getKey();
            int stock = entry.getValue().get();
            int reorderPoint = reorderPoints.get(product);
            int reorderQuantity = reorderQuantities.get(product);
            
            report.append(String.format("- %s: %d units", product.getName(), stock));
            if (stock <= reorderPoint) {
                report.append(" (LOW STOCK - Reorder Point: ").append(reorderPoint)
                      .append(", Reorder Quantity: ").append(reorderQuantity).append(")");
            }
            if (product.isExpired()) {
                report.append(" (EXPIRED)");
                if (product instanceof FoodProduct) {
                    report.append(" - Expired on: ").append(((FoodProduct) product).getExpirationDate());
                }
            }
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
} 