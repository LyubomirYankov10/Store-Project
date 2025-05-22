package org.example.model.analytics;

import org.example.model.product.Product;
import org.example.model.receipt.Receipt;
import org.example.model.store.Cashier;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StoreAnalytics {
    private final List<Receipt> receipts;
    private final Map<Product, Integer> productSales;
    private final Map<Cashier, Double> cashierPerformance;
    private double totalRevenue;
    private double totalExpenses;
    private final LocalDateTime startDate;

    public StoreAnalytics() {
        this.receipts = new CopyOnWriteArrayList<>();
        this.productSales = new ConcurrentHashMap<>();
        this.cashierPerformance = new ConcurrentHashMap<>();
        this.totalRevenue = 0.0;
        this.totalExpenses = 0.0;
        this.startDate = LocalDateTime.now();
    }

    public synchronized void addReceipt(Receipt receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("Receipt cannot be null");
        }
        receipts.add(receipt);
        totalRevenue += receipt.getTotalAmount();
        
        // Update product sales
        for (Map.Entry<Product, Integer> entry : receipt.getItems().entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            productSales.merge(product, quantity, Integer::sum);
        }
        
        // Update cashier performance
        Cashier cashier = receipt.getCashier();
        cashierPerformance.merge(cashier, receipt.getTotalAmount(), Double::sum);
    }

    public synchronized void addExpense(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Expense amount cannot be negative");
        }
        totalExpenses += amount;
    }

    public synchronized double getProfit() {
        return totalRevenue - totalExpenses;
    }

    public synchronized double getProfitMargin() {
        return totalRevenue > 0 ? (getProfit() / totalRevenue) * 100 : 0;
    }

    public Map<Product, Integer> getTopSellingProducts(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        return productSales.entrySet().stream()
            .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }

    public Map<Cashier, Double> getTopPerformingCashiers(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        return cashierPerformance.entrySet().stream()
            .sorted(Map.Entry.<Cashier, Double>comparingByValue().reversed())
            .limit(limit)
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
    }

    public synchronized double getAverageTransactionValue() {
        return receipts.isEmpty() ? 0 : totalRevenue / receipts.size();
    }

    public synchronized int getTotalTransactions() {
        return receipts.size();
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public synchronized double getTotalExpenses() {
        return totalExpenses;
    }

    public synchronized double getTotalRevenue() {
        return totalRevenue;
    }

    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("Store Analytics Report\n");
        report.append("=====================\n");
        report.append("Period: ").append(startDate).append(" to ").append(LocalDateTime.now()).append("\n\n");
        
        report.append("Financial Summary:\n");
        report.append("-----------------\n");
        report.append(String.format("Total Revenue: $%.2f\n", getTotalRevenue()));
        report.append(String.format("Total Expenses: $%.2f\n", getTotalExpenses()));
        report.append(String.format("Net Profit: $%.2f\n", getProfit()));
        report.append(String.format("Profit Margin: %.2f%%\n\n", getProfitMargin()));
        
        report.append("Sales Performance:\n");
        report.append("-----------------\n");
        report.append(String.format("Total Transactions: %d\n", getTotalTransactions()));
        report.append(String.format("Average Transaction Value: $%.2f\n\n", getAverageTransactionValue()));
        
        report.append("Top Selling Products:\n");
        report.append("--------------------\n");
        getTopSellingProducts(5).forEach((product, quantity) ->
            report.append(String.format("- %s: %d units\n", product.getName(), quantity)));
        report.append("\n");
        
        report.append("Top Performing Cashiers:\n");
        report.append("----------------------\n");
        getTopPerformingCashiers(3).forEach((cashier, sales) ->
            report.append(String.format("- %s: $%.2f in sales\n", cashier.getName(), sales)));
        
        return report.toString();
    }
} 