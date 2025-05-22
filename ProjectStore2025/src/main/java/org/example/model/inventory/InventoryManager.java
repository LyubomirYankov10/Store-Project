package org.example.model.inventory;

import org.example.model.product.Product;
import org.example.model.product.FoodProduct;
import org.example.exception.ProductException;
import java.time.LocalDate;
import java.util.*;

public class InventoryManager {
    private Map<Product, Integer> stockLevels;
    private Map<Product, Integer> reorderPoints;
    private Map<Product, Integer> reorderQuantities;
    private List<Product> lowStockProducts;
    private List<Product> expiredProducts;

    public InventoryManager() {
        this.stockLevels = new HashMap<>();
        this.reorderPoints = new HashMap<>();
        this.reorderQuantities = new HashMap<>();
        this.lowStockProducts = new ArrayList<>();
        this.expiredProducts = new ArrayList<>();
    }

    public void addProduct(Product product, int initialStock, int reorderPoint, int reorderQuantity) {
        if (product == null) {
            throw new ProductException("Cannot add null product to inventory");
        }
        if (initialStock < 0 || reorderPoint < 0 || reorderQuantity <= 0) {
            throw new ProductException("Invalid stock or reorder parameters");
        }

        stockLevels.put(product, initialStock);
        reorderPoints.put(product, reorderPoint);
        reorderQuantities.put(product, reorderQuantity);
        checkStockLevel(product);
    }

    public void updateStock(Product product, int quantity) {
        if (!stockLevels.containsKey(product)) {
            throw new ProductException("Product not found in inventory: " + product.getName());
        }

        int currentStock = stockLevels.get(product);
        int newStock = currentStock + quantity;
        
        if (newStock < 0) {
            throw new ProductException("Insufficient stock for product: " + product.getName());
        }

        stockLevels.put(product, newStock);
        checkStockLevel(product);
    }

    public int getStockLevel(Product product) {
        return stockLevels.getOrDefault(product, 0);
    }

    public boolean needsReorder(Product product) {
        return stockLevels.getOrDefault(product, 0) <= reorderPoints.getOrDefault(product, 0);
    }

    public int getReorderQuantity(Product product) {
        return reorderQuantities.getOrDefault(product, 0);
    }

    public List<Product> getLowStockProducts() {
        return new ArrayList<>(lowStockProducts);
    }

    public List<Product> getExpiredProducts() {
        return new ArrayList<>(expiredProducts);
    }

    private void checkStockLevel(Product product) {
        // Check for low stock
        if (needsReorder(product)) {
            if (!lowStockProducts.contains(product)) {
                lowStockProducts.add(product);
            }
        } else {
            lowStockProducts.remove(product);
        }

        // Check for expired products
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
        stockLevels.forEach((product, quantity) -> {
            report.append(String.format("- %s: %d units", product.getName(), quantity));
            if (needsReorder(product)) {
                report.append(" (LOW STOCK - Reorder Point: ").append(reorderPoints.get(product))
                      .append(", Reorder Quantity: ").append(reorderQuantities.get(product))
                      .append(")");
            }
            if (product.isExpired()) {
                report.append(" (EXPIRED)");
                if (product instanceof FoodProduct) {
                    report.append(" - Expired on: ").append(((FoodProduct) product).getExpirationDate());
                }
            }
            report.append("\n");
        });
        report.append("\n");

        if (!lowStockProducts.isEmpty()) {
            report.append("Low Stock Products:\n");
            report.append("------------------\n");
            lowStockProducts.forEach(product ->
                report.append(String.format("- %s: %d units (Reorder Point: %d)\n",
                    product.getName(),
                    stockLevels.get(product),
                    reorderPoints.get(product))));
            report.append("\n");
        }

        if (!expiredProducts.isEmpty()) {
            report.append("Expired Products:\n");
            report.append("----------------\n");
            expiredProducts.forEach(product -> {
                report.append(String.format("- %s: %d units", product.getName(), stockLevels.get(product)));
                if (product instanceof FoodProduct) {
                    report.append(" (Expired on: ").append(((FoodProduct) product).getExpirationDate()).append(")");
                }
                report.append("\n");
            });
        }

        return report.toString();
    }
} 