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
import java.util.concurrent.atomic.AtomicInteger;

public class StoreAnalytics {
    private final List<Receipt> receipts;
    private final Map<Product, AtomicInteger> productSales;
    private final Map<Cashier, AtomicInteger> cashierTransactions;
    private final Map<Cashier, AtomicReference<Double>> cashierRevenue;
    private final AtomicReference<Double> totalRevenue;
    private final AtomicReference<Double> totalExpenses;
    private final LocalDateTime startDate;
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private static final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StoreAnalytics() {
        this.receipts = new CopyOnWriteArrayList<>();
        this.productSales = new ConcurrentHashMap<>();
        this.cashierTransactions = new ConcurrentHashMap<>();
        this.cashierRevenue = new ConcurrentHashMap<>();
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
        
        for (Map.Entry<Product, Integer> entry : receipt.getItems().entrySet()) {
            Product product = entry.getKey();
            int quantity = entry.getValue();
            productSales.computeIfAbsent(product, k -> new AtomicInteger(0))
                       .addAndGet(quantity);
        }
        
        Cashier cashier = receipt.getCashier();
        cashierTransactions.computeIfAbsent(cashier, k -> new AtomicInteger(0))
                          .incrementAndGet();
        cashierRevenue.computeIfAbsent(cashier, k -> new AtomicReference<>(0.0))
                     .updateAndGet(current -> current + receipt.getTotalAmount());
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
            .sorted(Map.Entry.<Product, AtomicInteger>comparingByValue(Comparator.comparingInt(AtomicInteger::get)).reversed())
            .limit(limit)
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().get()), HashMap::putAll);
    }

    public Map<Cashier, Double> getTopPerformingCashiers(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("Limit cannot be negative");
        }
        return cashierRevenue.entrySet().stream()
            .sorted(Map.Entry.<Cashier, AtomicReference<Double>>comparingByValue(Comparator.comparingDouble(AtomicReference<Double>::get)).reversed())
            .limit(limit)
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().get()), HashMap::putAll);
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

    public int getProductSales(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        return productSales.getOrDefault(product, new AtomicInteger(0)).get();
    }

    public int getCashierTransactions(Cashier cashier) {
        if (cashier == null) {
            throw new IllegalArgumentException("Cashier cannot be null");
        }
        return cashierTransactions.getOrDefault(cashier, new AtomicInteger(0)).get();
    }

    public double getCashierRevenue(Cashier cashier) {
        if (cashier == null) {
            throw new IllegalArgumentException("Cashier cannot be null");
        }
        return cashierRevenue.getOrDefault(cashier, new AtomicReference<>(0.0)).get();
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