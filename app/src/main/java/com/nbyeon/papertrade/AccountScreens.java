package com.nbyeon.papertrade;

import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import static com.nbyeon.papertrade.MainActivity.*;

/** Stock detail, holdings and order-history screens. */
final class AccountScreens extends UiKit {
    AccountScreens(MainActivity activity) { super(activity); }
    void detail(DemoBroker.Stock stock) {
        LinearLayout toolbar = row();
        toolbar.addView(button("‹ Back", SURFACE, TEXT, () -> { a.selected = null; a.render(); }));
        TextView exchange = text("NASDAQ · USD", 11, MUTED, true); exchange.setGravity(Gravity.CENTER); weighted(toolbar, exchange);
        TextView star = button(a.favorites.contains(stock.symbol) ? "★" : "☆", SURFACE, ACCENT, () -> {
            if (!a.favorites.remove(stock.symbol)) a.favorites.add(stock.symbol);
            a.getPreferences(MainActivity.MODE_PRIVATE).edit().putStringSet("favorites", new HashSet<>(a.favorites)).apply(); a.render();
        });
        star.setContentDescription(a.favorites.contains(stock.symbol) ? "Remove from watchlist" : "Add to watchlist");
        toolbar.addView(star); a.body.addView(toolbar); space(a.body, 27);
        a.body.addView(text(stock.name, 28, TEXT, true)); space(a.body, 5);
        a.body.addView(text(stock.symbol + "  ·  " + stock.sector, 12, MUTED, false)); space(a.body, 20);
        a.body.addView(text(money(stock.cents), 43, TEXT, true)); space(a.body, 4);
        a.body.addView(text(String.format(Locale.US, "%+.2f%%  today  ·  Simulated quote", stock.change),
            13, stock.change >= 0 ? GREEN : RED, true)); space(a.body, 25);
        LinearLayout chartHeading = row(); weighted(chartHeading, text("PRICE OVERVIEW", 10, MUTED, true));
        chartHeading.addView(button(a.candles ? "Candles ▾" : "Line ▾", SURFACE, TEXT, () -> { a.candles = !a.candles; a.render(); }));
        a.body.addView(chartHeading); space(a.body, 8);
        ChartView chart = new ChartView(a, 41, stock.change >= 0 ? GREEN : RED, false);
        String[] ranges = {"1D", "1W", "1M", "3M", "1Y", "ALL"};
        chart.setRange(Arrays.asList(ranges).indexOf(a.range)); chart.setCandles(a.candles);
        a.body.addView(chart, new LinearLayout.LayoutParams(-1, dp(220))); space(a.body, 15);
        LinearLayout times = row();
        for (String r : ranges) {
            TextView t = button(r, r.equals(a.range) ? 0xff293722 : BG, r.equals(a.range) ? ACCENT : MUTED,
                () -> { a.range = r; a.render(); });
            times.addView(t, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        a.body.addView(times); space(a.body, 22);
        LinearLayout stats = card();
        dataRow(stats, "Quote source", "Server demo");
        dataRow(stats, "Order type", "Market · immediate fill");
        dataRow(stats, "Buying power", money(a.broker.cash));
        DemoBroker.Position p = a.broker.positions.get(stock.symbol);
        dataRow(stats, "Your position", (p == null ? 0 : p.quantity) + " shares");
        a.body.addView(stats); space(a.body, 22);
        a.body.addView(text("Paper trading only. Charts are synthetic illustrations.", 11, MUTED, false));
    }
    void portfolio() {
        title("Portfolio", "Your practice. Your progress.");
        LinearLayout balance = card();
        balance.addView(text("TOTAL ACCOUNT VALUE", 10, MUTED, true)); space(balance, 13);
        balance.addView(text(money(a.broker.cash + a.broker.marketValue()), 35, TEXT, true)); space(balance, 8);
        long gain = a.broker.marketValue() - a.broker.costBasis();
        balance.addView(text((gain >= 0 ? "+" : "") + money(gain) + " unrealized P&L", 13, gain >= 0 ? GREEN : RED, true));
        space(balance, 18); divider(balance);
        dataRow(balance, "Cash / buying power", money(a.broker.cash));
        dataRow(balance, "Invested market value", money(a.broker.marketValue()));
        a.body.addView(balance); space(a.body, 27); heading("Your positions", a.broker.positions.size() + " holdings");
        if (a.broker.positions.isEmpty()) {
            a.body.addView(text("Your portfolio starts with one move.", 16, TEXT, true)); space(a.body, 12);
            a.body.addView(button("Explore markets", ACCENT, BG, () -> { a.section = "Markets"; a.render(); }));
        }
        for (Map.Entry<String, DemoBroker.Position> entry : a.broker.positions.entrySet()) {
            DemoBroker.Stock s = DemoBroker.stock(entry.getKey()); DemoBroker.Position p = entry.getValue();
            LinearLayout holding = card(); LinearLayout line = row();
            weighted(line, text(s.symbol, 17, TEXT, true)); line.addView(text(money(s.cents * p.quantity), 17, TEXT, true));
            holding.addView(line); space(holding, 9);
            dataRow(holding, p.quantity + " shares · " + s.name, money(p.cost / p.quantity) + " avg");
            long profit = s.cents * p.quantity - p.cost;
            holding.addView(text((profit >= 0 ? "+" : "") + money(profit) + " unrealized", 12, profit >= 0 ? GREEN : RED, true));
            holding.setOnClickListener(v -> a.openStock(s.symbol)); holding.setFocusable(true);
            a.body.addView(holding); space(a.body, 12);
        }
        space(a.body, 12); a.body.addView(text(a.online ? "Virtual holdings · Synced from server" : "Cached virtual holdings · Server unavailable", 11, MUTED, false));
    }
    void orders() {
        title("Orders", "Every move, in one place.");
        heading("Activity", a.broker.orders.size() + " filled");
        if (a.broker.orders.isEmpty()) {
            space(a.body, 50);
            TextView mark = text("≡", 56, ACCENT, false); mark.setGravity(Gravity.CENTER); a.body.addView(mark); space(a.body, 20);
            TextView label = text("Your first move awaits", 23, TEXT, true); label.setGravity(Gravity.CENTER); a.body.addView(label); space(a.body, 12);
            TextView hint = text("Place a paper trade to see your\nexecution details here.", 14, MUTED, false);
            hint.setGravity(Gravity.CENTER); a.body.addView(hint); space(a.body, 28);
            a.body.addView(button("Find your first stock", ACCENT, BG, () -> { a.section = "Markets"; a.render(); }));
        }
        SimpleDateFormat date = new SimpleDateFormat("MMM d, HH:mm", Locale.US);
        for (DemoBroker.Order o : a.broker.orders) {
            LinearLayout item = card(); LinearLayout top = row();
            weighted(top, text((o.buy ? "Buy " : "Sell ") + o.symbol, 17, TEXT, true));
            top.addView(text("FILLED", 10, GREEN, true)); item.addView(top); space(item, 12);
            dataRow(item, o.quantity + " shares · Market", money(o.total));
            item.addView(text(date.format(new Date(o.time)) + " · Paper trade", 11, MUTED, false));
            item.setOnClickListener(v -> a.openStock(o.symbol)); a.body.addView(item); space(a.body, 12);
        }
    }
}
