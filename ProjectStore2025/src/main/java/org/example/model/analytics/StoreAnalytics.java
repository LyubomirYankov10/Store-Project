package org.example.model.analytics;

import org.example.model.product.Product;
import org.example.model.receipt.Receipt;
import org.example.model.store.Cashier;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.text.NumberFormat;
import java.util.Locale;

public class StoreAnalytics {
    private final List<Receipt> receipts;
    private final Map<Product, Integer> productSales;
    private final Map<Cashier, Double> cashierPerformance;
    private final AtomicReference<Double> totalRevenue;
    private final AtomicReference<Double> totalExpenses;
    private final LocalDateTime startDate;
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StoreAnalytics() {
        this.receipts = new CopyOnWriteArrayList<>();
        this.productSales = new ConcurrentHashMap<>();
        this.cashierPerformance = new ConcurrentHashMap<>();
        this.totalRevenue = new AtomicReference<>(0.0);
        this.totalExpenses = new AtomicReference<>(0.0);
        this.startDate = LocalDateTime.now();
    }

    public void addReceipt(Receipt receipt) {
        if (receipt == null) {
            throw new IllegalArgumentException("Receipt cannot be null");
        }
        receipts.add(receipt);
        totalRevenue.updateAndGet(current -> current + receipt.getTotalAmount());
        
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

    public void addExpense(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Expense amount cannot be negative");
        }
        totalExpenses.updateAndGet(current -> current + amount);
    }

    public double getProfit() {
        return totalRevenue.get() - totalExpenses.get();
    }

    public double getProfitMargin() {
        double revenue = totalRevenue.get();
        return revenue > 0 ? (getProfit() / revenue) * 100 : 0;
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

    public double getAverageTransactionValue() {
        int size = receipts.size();
        return size > 0 ? totalRevenue.get() / size : 0;
    }

    public int getTotalTransactions() {
        return receipts.size();
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public double getTotalExpenses() {
        return totalExpenses.get();
    }

    public double getTotalRevenue() {
        return totalRevenue.get();
    }

    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("Store Analytics Report\n");
        report.append("=====================\n");
        report.append("Period: ").append(startDate.format(dateFormatter))
              .append(" to ").append(LocalDateTime.now().format(dateFormatter)).append("\n\n");
        
        report.append("Financial Summary:\n");
        report.append("-----------------\n");
        report.append(String.format("Total Revenue: %s\n", currencyFormat.format(getTotalRevenue())));
        report.append(String.format("Total Expenses: %s\n", currencyFormat.format(getTotalExpenses())));
        report.append(String.format("Net Profit: %s\n", currencyFormat.format(getProfit())));
        report.append(String.format("Profit Margin: %.2f%%\n\n", getProfitMargin()));
        
        report.append("Sales Performance:\n");
        report.append("-----------------\n");
        report.append(String.format("Total Transactions: %d\n", getTotalTransactions()));
        report.append(String.format("Average Transaction Value: %s\n\n", 
            currencyFormat.format(getAverageTransactionValue())));
        
        report.append("Top Selling Products:\n");
        report.append("--------------------\n");
        getTopSellingProducts(5).forEach((product, quantity) ->
            report.append(String.format("- %s: %d units\n", product.getName(), quantity)));
        report.append("\n");
        
        report.append("Top Performing Cashiers:\n");
        report.append("----------------------\n");
        getTopPerformingCashiers(3).forEach((cashier, sales) ->
            report.append(String.format("- %s: %s in sales\n", 
                cashier.getName(), currencyFormat.format(sales))));
        
        return report.toString();
    }
} 