package com.nbyeon.papertrade;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Local demo only. A production service must own prices, authorization and transactions. */
public final class DemoBroker {
    public static final class Stock {
        public final String symbol, name, sector;
        public final long cents;
        public final double change;
        Stock(String symbol, String name, String sector, long cents, double change) {
            this.symbol = symbol; this.name = name; this.sector = sector;
            this.cents = cents; this.change = change;
        }
    }
    public static final Stock[] STOCKS = {
        new Stock("NVDA", "NVIDIA", "Semiconductors", 12784, 2.34),
        new Stock("AAPL", "Apple", "Consumer technology", 22852, 0.87),
        new Stock("TSLA", "Tesla", "Electric vehicles", 24850, -1.26),
        new Stock("MSFT", "Microsoft", "Software & cloud", 42876, 1.12),
        new Stock("AMZN", "Amazon", "Commerce & cloud", 18649, -0.43),
        new Stock("GOOGL", "Alphabet", "Internet services", 16585, 1.68)
    };
    public static final class Position {
        public int quantity;
        public long cost;
        Position(int quantity, long cost) { this.quantity = quantity; this.cost = cost; }
    }
    public static final class Order {
        public final String id, symbol;
        public final boolean buy;
        public final int quantity;
        public final long total, time;
        Order(String id, String symbol, boolean buy, int quantity, long total, long time) {
            this.id = id; this.symbol = symbol; this.buy = buy;
            this.quantity = quantity; this.total = total; this.time = time;
        }
    }
    public long cash = 2475000;
    public final Map<String, Position> positions = new LinkedHashMap<>();
    public final List<Order> orders = new ArrayList<>();

    public DemoBroker() {
        positions.put("NVDA", new Position(12, 139200));
        positions.put("AAPL", new Position(8, 176000));
        positions.put("MSFT", new Position(5, 205000));
    }
    public static Stock stock(String symbol) {
        for (Stock s : STOCKS) if (s.symbol.equals(symbol)) return s;
        throw new IllegalArgumentException("Unknown stock");
    }
    public synchronized Order execute(String id, String symbol, boolean buy, int quantity) {
        for (Order o : orders) if (o.id.equals(id)) {
            if (!o.symbol.equals(symbol) || o.buy != buy || o.quantity != quantity)
                throw new IllegalArgumentException("Request ID already used for another order.");
            return o;
        }
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("Missing order ID.");
        if (quantity < 1 || quantity > 1000000) throw new IllegalArgumentException("Enter 1 to 1,000,000 shares.");
        Stock s = stock(symbol);
        long total = Math.multiplyExact(s.cents, quantity);
        Position p = positions.get(symbol);
        if (buy && total > cash) throw new IllegalArgumentException("Not enough buying power. Try fewer shares.");
        if (!buy && (p == null || p.quantity < quantity))
            throw new IllegalArgumentException("Not enough shares to sell.");
        if (buy) {
            if (p == null) { p = new Position(0, 0); positions.put(symbol, p); }
            p.quantity += quantity;
            p.cost += total;
            cash -= total;
        } else {
            p.cost -= Math.round((double) p.cost * quantity / p.quantity);
            p.quantity -= quantity;
            cash += total;
            if (p.quantity == 0) positions.remove(symbol);
        }
        Order order = new Order(id, symbol, buy, quantity, total, System.currentTimeMillis());
        orders.add(0, order);
        return order;
    }
    public long marketValue() {
        long value = 0;
        for (Map.Entry<String, Position> e : positions.entrySet())
            value += stock(e.getKey()).cents * e.getValue().quantity;
        return value;
    }
    public long costBasis() {
        long value = 0;
        for (Position p : positions.values()) value += p.cost;
        return value;
    }
}

