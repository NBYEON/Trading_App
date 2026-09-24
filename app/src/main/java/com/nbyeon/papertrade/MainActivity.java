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

/** Navigation host for the paper-trading screens. The server owns account state. */
public final class MainActivity extends Activity {
    static final int BG = 0xff0b0e13, SURFACE = 0xff141922, BORDER = 0xff252c38;
    static final int TEXT = 0xfff0f3f8, MUTED = 0xff8b97aa, GREEN = 0xff53d6a0;
    static final int ACCENT = 0xffb2f36d, RED = 0xffff7088;
    LinearLayout root, body, nav;
    DemoBroker broker;
    final Set<String> favorites = new HashSet<>();
    String section = "Markets", selected;
    Dialog orderDialog;
    String range = "1D";
    boolean candles;
    private final UiKit ui = new UiKit(this);
    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);
    static TradingGateway testGateway;
    TradingGateway gateway;
    boolean online;
    String connectionMessage = "Connecting to demo server…";

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
        gateway = testGateway != null ? testGateway : new ApiClient(BuildConfig.API_BASE_URL);
        render();
        refreshRemote();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("section", section); out.putString("selected", selected);
        out.putString("range", range); out.putBoolean("candles", candles);
    }
    @Override protected void onDestroy() {
        if (orderDialog != null) orderDialog.dismiss();
        if (gateway instanceof ApiClient) ((ApiClient) gateway).close();
        super.onDestroy();
    }
    static String money(long cents) { return MONEY.format(cents / 100.0); }
    void render() {
        root = ui.column(); root.setBackgroundColor(BG); root.setFocusableInTouchMode(true);
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
        body = ui.column(); body.setPadding(ui.dp(22), ui.dp(20), ui.dp(22), ui.dp(24));
        scroll.addView(body); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (!online) {
            LinearLayout notice = ui.row(); notice.setPadding(ui.dp(12), ui.dp(9), ui.dp(12), ui.dp(9));
            notice.setBackground(ui.shape(0xff2b2522, 10, 0));
            ui.weighted(notice, ui.text(connectionMessage, 12, ACCENT, false));
            notice.addView(ui.button("Retry", SURFACE, TEXT, this::refreshRemote));
            body.addView(notice); ui.space(body, 16);
        }
        if (selected != null) new AccountScreens(this).detail(DemoBroker.stock(selected));
        else switch (section) {
            case "Watchlist": new MarketScreens(this).watchlist(); break;
            case "Portfolio": new AccountScreens(this).portfolio(); break;
            case "Orders": new AccountScreens(this).orders(); break;
            default: new MarketScreens(this).markets();
        }
        if (selected != null) {
            DemoBroker.Stock stock = DemoBroker.stock(selected);
            LinearLayout tradeBar = ui.row(); tradeBar.setPadding(ui.dp(22), ui.dp(12), ui.dp(22), ui.dp(12));
            tradeBar.setBackgroundColor(BG);
            ui.weighted(tradeBar, ui.button("Buy " + stock.symbol, ACCENT, BG, () -> showOrder(stock, true)));
            ui.gap(tradeBar, 12); ui.weighted(tradeBar, ui.button("Sell", SURFACE, RED, () -> showOrder(stock, false)));
            ui.divider(root); root.addView(tradeBar);
        }
        ui.divider(root); navigation();
    }
    private void navigation() {
        nav = ui.row(); nav.setBackgroundColor(0xff10141c);
        String[] names = {"Markets", "Watchlist", "Portfolio", "Orders"};
        String[] icons = {"◷", "☆", "▥", "≡"};
        for (int i = 0; i < names.length; i++) {
            String name = names[i]; boolean active = name.equals(section);
            LinearLayout item = ui.column(); item.setGravity(Gravity.CENTER); item.setPadding(0, ui.dp(10), 0, ui.dp(10));
            TextView icon = ui.text(icons[i], 23, active ? ACCENT : MUTED, false); icon.setGravity(Gravity.CENTER);
            item.addView(icon); ui.space(item, 3);
            TextView label = ui.text(name, 10, active ? ACCENT : MUTED, active);
            label.setGravity(Gravity.CENTER); item.addView(label);
            item.setContentDescription(name + " tab"); item.setFocusable(true);
            item.setOnClickListener(v -> { section = name; selected = null; render(); });
            nav.addView(item, new LinearLayout.LayoutParams(0, ui.dp(72), 1));
        }
        root.addView(nav);
    }
    void openStock(String symbol) { selected = symbol; range = "1D"; candles = false; render(); }
    void showOrder(DemoBroker.Stock stock, boolean buy) { new OrderTicket(this).showOrder(stock, buy); }
    /** Refreshes the authoritative server snapshot; cached data is display-only when offline. */
    void refreshRemote() {
        connectionMessage = "Connecting to demo server…";
        gateway.load(new TradingGateway.Callback<DemoBroker>() {
            public void success(DemoBroker snapshot) {
                if (isDestroyed()) return;
                broker = snapshot; online = true; DemoStore.save(MainActivity.this, broker); render();
            }
            public void failure(String message) {
                if (isDestroyed()) return;
                online = false;
                connectionMessage = "Server unavailable · " + message;
                render();
            }
        });
    }
    @Override public void onBackPressed() {
        if (selected != null) { selected = null; render(); }
        else if (!section.equals("Markets")) { section = "Markets"; render(); }
        else super.onBackPressed();
    }
}


