package com.nbyeon.papertrade.api;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import static com.nbyeon.papertrade.api.Models.*;

@Repository
public class TradingRepository {
    private final JdbcTemplate jdbc;
    TradingRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    List<Stock> stocks() {
        return jdbc.query("SELECT symbol,name,sector,price_cents,change_percent FROM instruments ORDER BY symbol",
            (rs, n) -> new Stock(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getDouble(5)));
    }
    Stock stock(String symbol) {
        List<Stock> matches = jdbc.query("SELECT symbol,name,sector,price_cents,change_percent FROM instruments WHERE symbol=?",
            (rs, n) -> new Stock(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getDouble(5)), symbol);
        return matches.isEmpty() ? null : matches.get(0);
    }
    /** Joins the normalized account tables into the snapshot returned to Android. */
    Account account() {
        Long cash = jdbc.queryForObject("SELECT cash_cents FROM accounts WHERE id=1", Long.class);
        List<Position> positions = jdbc.query("SELECT symbol,quantity,cost_cents FROM positions WHERE account_id=1 ORDER BY symbol",
            (rs, n) -> new Position(rs.getString(1), rs.getInt(2), rs.getLong(3)));
        List<Order> orders = jdbc.query("SELECT request_id,symbol,side,quantity,total_cents,filled_at FROM orders WHERE account_id=1 ORDER BY filled_at DESC,request_id DESC LIMIT 100",
            (rs, n) -> mapOrder(rs));
        return new Account(cash, positions, orders);
    }
    /** Serializes orders for the demo account, so two requests cannot spend the same cash. */
    long lockedCash() {
        return jdbc.queryForObject("SELECT cash_cents FROM accounts WHERE id=1 FOR UPDATE", Long.class);
    }
    Order existing(UUID id) {
        List<Order> orders = jdbc.query("SELECT request_id,symbol,side,quantity,total_cents,filled_at FROM orders WHERE request_id=?",
            (rs, n) -> mapOrder(rs), id);
        return orders.isEmpty() ? null : orders.get(0);
    }
    Position position(String symbol) {
        List<Position> positions = jdbc.query("SELECT symbol,quantity,cost_cents FROM positions WHERE account_id=1 AND symbol=?",
            (rs, n) -> new Position(rs.getString(1), rs.getInt(2), rs.getLong(3)), symbol);
        return positions.isEmpty() ? null : positions.get(0);
    }
    void setCash(long cents) { jdbc.update("UPDATE accounts SET cash_cents=? WHERE id=1", cents); }
    void upsertPosition(String symbol, int quantity, long cost) {
        jdbc.update("INSERT INTO positions(account_id,symbol,quantity,cost_cents) VALUES (1,?,?,?) " +
            "ON CONFLICT (account_id,symbol) DO UPDATE SET quantity=EXCLUDED.quantity,cost_cents=EXCLUDED.cost_cents",
            symbol, quantity, cost);
    }
    void deletePosition(String symbol) { jdbc.update("DELETE FROM positions WHERE account_id=1 AND symbol=?", symbol); }
    Order record(UUID id, String symbol, boolean buy, int quantity, long total) {
        return jdbc.queryForObject("INSERT INTO orders(request_id,account_id,symbol,side,quantity,total_cents) " +
                "VALUES (?,1,?,?,?,?) RETURNING request_id,symbol,side,quantity,total_cents,filled_at",
            (rs, n) -> mapOrder(rs), id, symbol, buy ? "BUY" : "SELL", quantity, total);
    }
    private Order mapOrder(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new Order(rs.getObject(1, UUID.class), rs.getString(2), "BUY".equals(rs.getString(3)),
            rs.getInt(4), rs.getLong(5), rs.getTimestamp(6).getTime());
    }
}
