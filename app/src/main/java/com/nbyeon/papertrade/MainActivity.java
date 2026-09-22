package com.nbyeon.papertrade;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.ViewGroup;
import android.widget.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/** A native Android, offline UI prototype. No brokerage connections or real orders. */
public final class MainActivity extends Activity {
    static final int BG = 0xff0b0e13, SURFACE = 0xff141922, BORDER = 0xff252c38;
    static final int TEXT = 0xfff0f3f8, MUTED = 0xff8b97aa, GREEN = 0xff53d6a0;
    static final int ACCENT = 0xffb2f36d, RED = 0xffff7088;
    private LinearLayout root, body, nav;
    private DemoBroker broker;
    private final Set<String> favorites = new HashSet<>();
    private String section = "Markets", selected;
    private Dialog orderDialog;
    private String range = "1D";
    private boolean candles;
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (android.os.Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        try { broker = DemoStore.load(this); }
        catch (IllegalStateException e) {
            broker = new DemoBroker();
            new AlertDialog.Builder(this).setTitle("Saved account unavailable")
                .setMessage("A fresh demo account is displayed. Previous saved data could not be read.")
                .setPositiveButton("Continue", null).show();
        }
        favorites.addAll(getPreferences(MODE_PRIVATE).getStringSet("favorites",
            new HashSet<>(Arrays.asList("NVDA", "AAPL", "TSLA", "MSFT"))));
        if (state != null) {
            section = state.getString("section", "Markets");
            selected = state.getString("selected");
            range = state.getString("range", "1D");
            candles = state.getBoolean("candles");
        }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("section", section); out.putString("selected", selected);
        out.putString("range", range); out.putBoolean("candles", candles);
    }
    @Override protected void onDestroy() {
        if (orderDialog != null) orderDialog.dismiss();
        super.onDestroy();
    }
    static String money(long cents) { return MONEY.format(cents / 100.0); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color); drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }
    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v;
    }
    private LinearLayout row() {
        LinearLayout v = new LinearLayout(this); v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setFontFeatureSettings("tnum");
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return v;
    }
    private void space(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }
    private void divider(LinearLayout parent) {
        View line = new View(this); line.setBackgroundColor(BORDER);
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }
    private void weighted(LinearLayout parent, View child) {
        parent.addView(child, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private TextView button(String label, int fill, int color, Runnable action) {
        TextView v = text(label, 14, color, true);
        v.setGravity(Gravity.CENTER); v.setMinHeight(dp(48));
        v.setPadding(dp(12), dp(10), dp(12), dp(10)); v.setBackground(shape(fill, 12, 0));
        v.setOnClickListener(view -> action.run()); v.setFocusable(true);
        v.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info); info.setClassName("android.widget.Button");
            }
        });
        return v;
    }
    private void gap(LinearLayout parent, int width) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(dp(width), 1));
    }
    private void title(String name, String subtitle) {
        LinearLayout header = row();
        LinearLayout labels = column();
        labels.addView(text(name, 29, TEXT, true));
        if (subtitle != null) { space(labels, 5); labels.addView(text(subtitle, 12, MUTED, false)); }
        weighted(header, labels);
        TextView badge = text("PAPER", 10, ACCENT, true);
        badge.setPadding(dp(10), dp(7), dp(10), dp(7)); badge.setBackground(shape(0xff253021, 7, 0));
        badge.setContentDescription("Paper trading demo");
        header.addView(badge); body.addView(header); space(body, 25);
    }
    private void heading(String label, String caption) {
        LinearLayout line = row(); weighted(line, text(label, 19, TEXT, true));
        if (caption != null) line.addView(text(caption, 11, MUTED, false));
        body.addView(line); space(body, 15);
    }
    private LinearLayout card() {
        LinearLayout v = column(); v.setPadding(dp(16), dp(16), dp(16), dp(16));
        v.setBackground(shape(SURFACE, 15, BORDER)); return v;
    }
    private void render() {
        root = column(); root.setBackgroundColor(BG); root.setFocusableInTouchMode(true);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        root.requestFocus();
        root.requestApplyInsets();
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setVerticalScrollBarEnabled(false);
        body = column(); body.setPadding(dp(22), dp(20), dp(22), dp(24));
        scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (selected != null) detail(DemoBroker.stock(selected));
        else switch (section) {
            case "Watchlist": watchlist(); break;
            case "Portfolio": portfolio(); break;
            case "Orders": orders(); break;
            default: markets();
        }
        if (selected != null) {
            DemoBroker.Stock stock = DemoBroker.stock(selected);
            LinearLayout tradeBar = row(); tradeBar.setPadding(dp(22), dp(12), dp(22), dp(12));
            tradeBar.setBackgroundColor(BG);
            weighted(tradeBar, button("Buy " + stock.symbol, ACCENT, BG, () -> showOrder(stock, true)));
            gap(tradeBar, 12); weighted(tradeBar, button("Sell", SURFACE, RED, () -> showOrder(stock, false)));
            divider(root); root.addView(tradeBar);
        }
        divider(root); navigation();
    }
    private void navigation() {
        nav = row(); nav.setBackgroundColor(0xff10141c);
        String[] names = {"Markets", "Watchlist", "Portfolio", "Orders"};
        String[] icons = {"◷", "☆", "▥", "≡"};
        for (int i = 0; i < names.length; i++) {
            String name = names[i]; boolean active = name.equals(section);
            LinearLayout item = column(); item.setGravity(Gravity.CENTER); item.setPadding(0, dp(10), 0, dp(10));
            TextView icon = text(icons[i], 23, active ? ACCENT : MUTED, false); icon.setGravity(Gravity.CENTER);
            item.addView(icon); space(item, 3);
            TextView label = text(name, 10, active ? ACCENT : MUTED, active);
            label.setGravity(Gravity.CENTER); item.addView(label);
            item.setContentDescription(name + " tab"); item.setFocusable(true);
            item.setOnClickListener(v -> { section = name; selected = null; render(); });
            nav.addView(item, new LinearLayout.LayoutParams(0, dp(72), 1));
        }
        root.addView(nav);
    }
    private void markets() {
        title("Markets", "Your next move starts here.");
        LinearLayout marketTabs = row();
        TextView us = text("US stocks", 14, ACCENT, true);
        us.setPadding(0, 0, dp(22), dp(12)); marketTabs.addView(us);
        marketTabs.addView(text("SIMULATED MARKET", 10, MUTED, true));
        body.addView(marketTabs); divider(body); space(body, 18);
        LinearLayout indices = row();
        index(indices, "S&P 500", "5,782.76", "+0.82%", 23);
        gap(indices, 8); index(indices, "NASDAQ", "18,239.92", "+1.24%", 7);
        gap(indices, 8); index(indices, "DOW JONES", "42,512.00", "+0.36%", 39);
        body.addView(indices); space(body, 26);
        LinearLayout feature = card();
        LinearLayout top = row(); weighted(top, text("ON YOUR RADAR", 10, ACCENT, true));
        top.addView(text("01 / 06", 10, MUTED, false)); feature.addView(top); space(feature, 14);
        LinearLayout featureRow = row(); LinearLayout info = column();
        info.addView(text("NVIDIA", 22, TEXT, true)); space(info, 4);
        info.addView(text("NVDA  ·  Semiconductors", 11, MUTED, false)); weighted(featureRow, info);
        LinearLayout price = column(); price.setGravity(Gravity.END);
        TextView featuredPrice = text("$127.84", 22, TEXT, true); featuredPrice.setGravity(Gravity.END);
        price.addView(featuredPrice); space(price, 4);
        TextView featuredChange = text("+2.34% today", 12, GREEN, true); featuredChange.setGravity(Gravity.END);
        price.addView(featuredChange);
        featureRow.addView(price, new LinearLayout.LayoutParams(dp(125), -2)); feature.addView(featureRow);
        ChartView chart = new ChartView(this, 41, GREEN, true);
        feature.addView(chart, new LinearLayout.LayoutParams(-1, dp(62)));
        TextView explore = text("Explore stock  →", 12, ACCENT, true); feature.addView(explore);
        feature.setOnClickListener(v -> openStock("NVDA")); feature.setFocusable(true);
        feature.setContentDescription("Explore NVIDIA stock, 127 dollars and 84 cents, up 2.34 percent");
        body.addView(feature); space(body, 26);
        heading("Market watch", "6 symbols");
        searchList(false);
        space(body, 18); body.addView(text("Prices and charts are illustrative. No real money.", 11, MUTED, false));
    }
    private void index(LinearLayout parent, String name, String value, String change, int seed) {
        LinearLayout tile = column(); tile.setPadding(dp(10), dp(12), dp(10), dp(8));
        tile.setBackground(shape(SURFACE, 10, 0));
        tile.addView(text(name, 9, MUTED, true)); space(tile, 7);
        tile.addView(text(value, 13, TEXT, true)); space(tile, 3);
        tile.addView(text(change, 11, GREEN, true));
        tile.addView(new ChartView(this, seed, GREEN, true), new LinearLayout.LayoutParams(-1, dp(31)));
        parent.addView(tile, new LinearLayout.LayoutParams(0, -2, 1));
    }
    private void watchlist() {
        title("Watchlist", "A little focus. A better perspective.");
        heading("My watchlist", favorites.size() + " symbols");
        searchList(true);
        space(body, 20);
        body.addView(text("Tap a stock to explore. Use the star on its detail page to update your list.", 12, MUTED, false));
    }
    private void searchList(boolean onlyFavorites) {
        EditText search = new EditText(this);
        search.setSingleLine(true); search.setTextSize(13); search.setTextColor(TEXT); search.setHintTextColor(MUTED);
        search.setHint("Search symbol or company"); search.setContentDescription("Search symbol or company");
        search.setPadding(dp(14), 0, dp(14), 0); search.setBackground(shape(SURFACE, 10, BORDER));
        body.addView(search, new LinearLayout.LayoutParams(-1, dp(46))); space(body, 15);
        LinearLayout labels = row(); weighted(labels, text("SYMBOL / NAME", 9, MUTED, true));
        labels.addView(text("LAST PRICE / CHANGE", 9, MUTED, true)); body.addView(labels); space(body, 8);
        LinearLayout list = column(); body.addView(list);
        fillStocks(list, "", onlyFavorites);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fillStocks(list, s.toString(), onlyFavorites);
            }
            public void afterTextChanged(Editable e) {}
        });
    }
    private void fillStocks(LinearLayout list, String query, boolean onlyFavorites) {
        list.removeAllViews(); String q = query.trim().toLowerCase(Locale.ROOT); int count = 0;
        for (DemoBroker.Stock s : DemoBroker.STOCKS) {
            if (onlyFavorites && !favorites.contains(s.symbol)) continue;
            if (!(s.symbol + " " + s.name).toLowerCase(Locale.ROOT).contains(q)) continue;
            count++;
            LinearLayout line = row(); line.setPadding(0, dp(13), 0, dp(13));
            TextView avatar = text(s.symbol.substring(0, 1), 16, s.symbol.equals("NVDA") ? ACCENT : 0xffbfcae1, true);
            avatar.setGravity(Gravity.CENTER); avatar.setBackground(shape(0xff202837, 12, 0));
            line.addView(avatar, new LinearLayout.LayoutParams(dp(38), dp(38))); gap(line, 11);
            LinearLayout names = column(); names.addView(text(s.symbol, 14, TEXT, true)); space(names, 4);
            names.addView(text(s.name, 11, MUTED, false)); weighted(line, names);
            ChartView spark = new ChartView(this, s.symbol.hashCode(), s.change >= 0 ? GREEN : RED, true);
            line.addView(spark, new LinearLayout.LayoutParams(dp(49), dp(30))); gap(line, 12);
            LinearLayout values = column(); values.setGravity(Gravity.END);
            values.addView(text(money(s.cents).substring(1), 14, TEXT, true)); space(values, 4);
            values.addView(text(String.format(Locale.US, "%+.2f%%", s.change), 11, s.change >= 0 ? GREEN : RED, true));
            line.addView(values, new LinearLayout.LayoutParams(dp(72), -2));
            line.setOnClickListener(v -> openStock(s.symbol)); line.setFocusable(true);
            list.addView(line); divider(list);
        }
        if (count == 0) { space(list, 24); list.addView(text("No symbols found", 17, TEXT, true)); space(list, 8);
            list.addView(text(onlyFavorites ? "Add stocks using the star on a stock page, or try another search." : "Try a symbol like NVDA or a company name.", 12, MUTED, false)); }
    }
    private void openStock(String symbol) { selected = symbol; range = "1D"; candles = false; render(); }
    private void detail(DemoBroker.Stock stock) {
        LinearLayout toolbar = row();
        toolbar.addView(button("‹ Back", SURFACE, TEXT, () -> { selected = null; render(); }));
        TextView exchange = text("NASDAQ · USD", 11, MUTED, true); exchange.setGravity(Gravity.CENTER); weighted(toolbar, exchange);
        TextView star = button(favorites.contains(stock.symbol) ? "★" : "☆", SURFACE, ACCENT, () -> {
            if (!favorites.remove(stock.symbol)) favorites.add(stock.symbol);
            getPreferences(MODE_PRIVATE).edit().putStringSet("favorites", new HashSet<>(favorites)).apply(); render();
        });
        star.setContentDescription(favorites.contains(stock.symbol) ? "Remove from watchlist" : "Add to watchlist");
        toolbar.addView(star); body.addView(toolbar); space(body, 27);
        body.addView(text(stock.name, 28, TEXT, true)); space(body, 5);
        body.addView(text(stock.symbol + "  ·  " + stock.sector, 12, MUTED, false)); space(body, 20);
        body.addView(text(money(stock.cents), 43, TEXT, true)); space(body, 4);
        body.addView(text(String.format(Locale.US, "%+.2f%%  today  ·  Simulated quote", stock.change),
            13, stock.change >= 0 ? GREEN : RED, true)); space(body, 25);
        LinearLayout chartHeading = row(); weighted(chartHeading, text("PRICE OVERVIEW", 10, MUTED, true));
        chartHeading.addView(button(candles ? "Candles ▾" : "Line ▾", SURFACE, TEXT, () -> { candles = !candles; render(); }));
        body.addView(chartHeading); space(body, 8);
        ChartView chart = new ChartView(this, 41, stock.change >= 0 ? GREEN : RED, false);
        String[] ranges = {"1D", "1W", "1M", "3M", "1Y", "ALL"};
        chart.setRange(Arrays.asList(ranges).indexOf(range)); chart.setCandles(candles);
        body.addView(chart, new LinearLayout.LayoutParams(-1, dp(220))); space(body, 15);
        LinearLayout times = row();
        for (String r : ranges) {
            TextView t = button(r, r.equals(range) ? 0xff293722 : BG, r.equals(range) ? ACCENT : MUTED,
                () -> { range = r; render(); });
            times.addView(t, new LinearLayout.LayoutParams(0, dp(48), 1));
        }
        body.addView(times); space(body, 22);
        LinearLayout stats = card();
        dataRow(stats, "Quote source", "Offline demo");
        dataRow(stats, "Order type", "Market · immediate fill");
        dataRow(stats, "Buying power", money(broker.cash));
        DemoBroker.Position p = broker.positions.get(stock.symbol);
        dataRow(stats, "Your position", (p == null ? 0 : p.quantity) + " shares");
        body.addView(stats); space(body, 22);
        body.addView(text("Paper trading only. Charts are synthetic illustrations.", 11, MUTED, false));
    }
    private void dataRow(LinearLayout parent, String label, String value) {
        LinearLayout line = row(); line.setPadding(0, dp(9), 0, dp(9));
        weighted(line, text(label, 12, MUTED, false)); line.addView(text(value, 12, TEXT, true)); parent.addView(line);
    }
    private void portfolio() {
        title("Portfolio", "Your practice. Your progress.");
        LinearLayout balance = card();
        balance.addView(text("TOTAL ACCOUNT VALUE", 10, MUTED, true)); space(balance, 13);
        balance.addView(text(money(broker.cash + broker.marketValue()), 35, TEXT, true)); space(balance, 8);
        long gain = broker.marketValue() - broker.costBasis();
        balance.addView(text((gain >= 0 ? "+" : "") + money(gain) + " unrealized P&L", 13, gain >= 0 ? GREEN : RED, true));
        space(balance, 18); divider(balance);
        dataRow(balance, "Cash / buying power", money(broker.cash));
        dataRow(balance, "Invested market value", money(broker.marketValue()));
        body.addView(balance); space(body, 27); heading("Your positions", broker.positions.size() + " holdings");
        if (broker.positions.isEmpty()) {
            body.addView(text("Your portfolio starts with one move.", 16, TEXT, true)); space(body, 12);
            body.addView(button("Explore markets", ACCENT, BG, () -> { section = "Markets"; render(); }));
        }
        for (Map.Entry<String, DemoBroker.Position> entry : broker.positions.entrySet()) {
            DemoBroker.Stock s = DemoBroker.stock(entry.getKey()); DemoBroker.Position p = entry.getValue();
            LinearLayout holding = card(); LinearLayout line = row();
            weighted(line, text(s.symbol, 17, TEXT, true)); line.addView(text(money(s.cents * p.quantity), 17, TEXT, true));
            holding.addView(line); space(holding, 9);
            dataRow(holding, p.quantity + " shares · " + s.name, money(p.cost / p.quantity) + " avg");
            long profit = s.cents * p.quantity - p.cost;
            holding.addView(text((profit >= 0 ? "+" : "") + money(profit) + " unrealized", 12, profit >= 0 ? GREEN : RED, true));
            holding.setOnClickListener(v -> openStock(s.symbol)); holding.setFocusable(true);
            body.addView(holding); space(body, 12);
        }
        space(body, 12); body.addView(text("Seeded virtual holdings · Stored on this device", 11, MUTED, false));
    }
    private void orders() {
        title("Orders", "Every move, in one place.");
        heading("Activity", broker.orders.size() + " filled");
        if (broker.orders.isEmpty()) {
            space(body, 50);
            TextView mark = text("≡", 56, ACCENT, false); mark.setGravity(Gravity.CENTER); body.addView(mark); space(body, 20);
            TextView label = text("Your first move awaits", 23, TEXT, true); label.setGravity(Gravity.CENTER); body.addView(label); space(body, 12);
            TextView hint = text("Place a paper trade to see your\nexecution details here.", 14, MUTED, false);
            hint.setGravity(Gravity.CENTER); body.addView(hint); space(body, 28);
            body.addView(button("Find your first stock", ACCENT, BG, () -> { section = "Markets"; render(); }));
        }
        SimpleDateFormat date = new SimpleDateFormat("MMM d, HH:mm", Locale.US);
        for (DemoBroker.Order o : broker.orders) {
            LinearLayout item = card(); LinearLayout top = row();
            weighted(top, text((o.buy ? "Buy " : "Sell ") + o.symbol, 17, TEXT, true));
            top.addView(text("FILLED", 10, GREEN, true)); item.addView(top); space(item, 12);
            dataRow(item, o.quantity + " shares · Market", money(o.total));
            item.addView(text(date.format(new Date(o.time)) + " · Paper trade", 11, MUTED, false));
            item.setOnClickListener(v -> openStock(o.symbol)); body.addView(item); space(body, 12);
        }
    }
    private void showOrder(DemoBroker.Stock stock, boolean buy) {
        orderDialog = new Dialog(this); orderDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(this); LinearLayout form = column();
        form.setPadding(dp(24), dp(24), dp(24), dp(24)); form.setBackground(shape(SURFACE, 22, BORDER)); scroll.addView(form);
        form.addView(text("PAPER ORDER", 10, ACCENT, true)); space(form, 14);
        form.addView(text((buy ? "Buy " : "Sell ") + stock.symbol, 27, TEXT, true)); space(form, 6);
        form.addView(text(stock.name + " · Market order", 13, MUTED, false)); space(form, 20);
        dataRow(form, "Price per share", money(stock.cents));
        dataRow(form, buy ? "Buying power" : "Available shares", buy ? money(broker.cash) :
            (broker.positions.containsKey(stock.symbol) ? broker.positions.get(stock.symbol).quantity : 0) + " shares");
        space(form, 18); form.addView(text("Quantity", 12, MUTED, true)); space(form, 8);
        EditText quantity = new EditText(this); quantity.setSingleLine(true);
        quantity.setInputType(InputType.TYPE_CLASS_NUMBER); quantity.setText("1"); quantity.setSelectAllOnFocus(true);
        quantity.setTextColor(TEXT); quantity.setTextSize(24); quantity.setContentDescription("Order quantity");
        quantity.setPadding(dp(14), dp(8), dp(14), dp(8)); quantity.setBackground(shape(BG, 10, BORDER));
        form.addView(quantity, new LinearLayout.LayoutParams(-1, dp(58))); space(form, 15);
        TextView total = text("Estimated total   " + money(stock.cents), 16, TEXT, true); form.addView(total);
        TextView error = text("", 12, RED, false); error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        form.addView(error); space(form, 15);
        quantity.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try { int n = Integer.parseInt(s.toString());
                    total.setText(n > 0 && n <= 1000000 ? "Estimated total   " + money(stock.cents * n) : "Enter 1 to 1,000,000 shares");
                } catch (NumberFormatException e) { total.setText("Enter a valid quantity"); }
                error.setText("");
            }
            public void afterTextChanged(Editable e) {}
        });
        String requestId = UUID.randomUUID().toString();
        form.addView(button("Review order", buy ? ACCENT : RED, BG, () -> {
            final int n;
            try { n = Integer.parseInt(quantity.getText().toString()); }
            catch (NumberFormatException e) { error.setText("Enter a whole number of shares."); return; }
            if (n < 1 || n > 1000000) { error.setText("Enter 1 to 1,000,000 shares."); return; }
            if (buy && stock.cents * n > broker.cash) { error.setText("Not enough buying power. Try fewer shares."); return; }
            if (!buy && (!broker.positions.containsKey(stock.symbol) || broker.positions.get(stock.symbol).quantity < n)) {
                error.setText("Not enough shares to sell."); return;
            }
            new AlertDialog.Builder(this).setTitle("Confirm paper " + (buy ? "buy" : "sell"))
                .setMessage(n + " shares of " + stock.symbol + " at " + money(stock.cents) +
                    "\nTotal: " + money(stock.cents * n) + "\n\nThis simulated order fills immediately. No real money is used.")
                .setNegativeButton("Go back", null)
                .setPositiveButton("Place paper order", (dialog, which) -> {
                    try {
                        // Work on a persisted snapshot so a failed disk write cannot leave partial UI state.
                        DemoBroker candidate = DemoStore.load(this);
                        candidate.execute(requestId, stock.symbol, buy, n);
                        if (!DemoStore.save(this, candidate)) { error.setText("Could not save this order. Please try again."); return; }
                        broker = candidate; orderDialog.dismiss(); selected = null; section = "Orders"; render();
                        Toast.makeText(this, "Paper order filled", Toast.LENGTH_SHORT).show();
                    } catch (IllegalArgumentException | IllegalStateException e) { error.setText(e.getMessage()); }
                }).show();
        }));
        space(form, 10); form.addView(button("Cancel", SURFACE, MUTED, () -> orderDialog.dismiss()));
        orderDialog.setContentView(scroll); orderDialog.show();
        Window window = orderDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(getResources().getDisplayMetrics().widthPixels - dp(24), ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }
    @Override public void onBackPressed() {
        if (selected != null) { selected = null; render(); }
        else if (!section.equals("Markets")) { section = "Markets"; render(); }
        else super.onBackPressed();
    }
}


