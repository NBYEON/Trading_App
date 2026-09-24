package com.nbyeon.papertrade;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;
import static com.nbyeon.papertrade.MainActivity.*;

/** Market and watchlist presentation; account updates stay outside this class. */
final class MarketScreens extends UiKit {
    MarketScreens(MainActivity activity) { super(activity); }
    void markets() {
        title("Markets", "Your next move starts here.");
        LinearLayout marketTabs = row();
        TextView us = text("US stocks", 14, ACCENT, true);
        us.setPadding(0, 0, dp(22), dp(12)); marketTabs.addView(us);
        marketTabs.addView(text("SIMULATED MARKET", 10, MUTED, true));
        a.body.addView(marketTabs); divider(a.body); space(a.body, 18);
        LinearLayout indices = row();
        index(indices, "S&P 500", "5,782.76", "+0.82%", 23);
        gap(indices, 8); index(indices, "NASDAQ", "18,239.92", "+1.24%", 7);
        gap(indices, 8); index(indices, "DOW JONES", "42,512.00", "+0.36%", 39);
        a.body.addView(indices); space(a.body, 26);
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
        ChartView chart = new ChartView(a, 41, GREEN, true);
        feature.addView(chart, new LinearLayout.LayoutParams(-1, dp(62)));
        TextView explore = text("Explore stock  →", 12, ACCENT, true); feature.addView(explore);
        feature.setOnClickListener(v -> a.openStock("NVDA")); feature.setFocusable(true);
        feature.setContentDescription("Explore NVIDIA stock, 127 dollars and 84 cents, up 2.34 percent");
        a.body.addView(feature); space(a.body, 26);
        heading("Market watch", "6 symbols");
        searchList(false);
        space(a.body, 18); a.body.addView(text("Prices and charts are illustrative. No real money.", 11, MUTED, false));
    }
    void index(LinearLayout parent, String name, String value, String change, int seed) {
        LinearLayout tile = column(); tile.setPadding(dp(10), dp(12), dp(10), dp(8));
        tile.setBackground(shape(SURFACE, 10, 0));
        tile.addView(text(name, 9, MUTED, true)); space(tile, 7);
        tile.addView(text(value, 13, TEXT, true)); space(tile, 3);
        tile.addView(text(change, 11, GREEN, true));
        tile.addView(new ChartView(a, seed, GREEN, true), new LinearLayout.LayoutParams(-1, dp(31)));
        parent.addView(tile, new LinearLayout.LayoutParams(0, -2, 1));
    }
    void watchlist() {
        title("Watchlist", "A little focus. A better perspective.");
        heading("My watchlist", a.favorites.size() + " symbols");
        searchList(true);
        space(a.body, 20);
        a.body.addView(text("Tap a stock to explore. Use the star on its detail page to update your list.", 12, MUTED, false));
    }
    void searchList(boolean onlyFavorites) {
        EditText search = new EditText(a);
        search.setSingleLine(true); search.setTextSize(13); search.setTextColor(TEXT); search.setHintTextColor(MUTED);
        search.setHint("Search symbol or company"); search.setContentDescription("Search symbol or company");
        search.setPadding(dp(14), 0, dp(14), 0); search.setBackground(shape(SURFACE, 10, BORDER));
        a.body.addView(search, new LinearLayout.LayoutParams(-1, dp(46))); space(a.body, 15);
        LinearLayout labels = row(); weighted(labels, text("SYMBOL / NAME", 9, MUTED, true));
        labels.addView(text("LAST PRICE / CHANGE", 9, MUTED, true)); a.body.addView(labels); space(a.body, 8);
        LinearLayout list = column(); a.body.addView(list);
        fillStocks(list, "", onlyFavorites);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                fillStocks(list, s.toString(), onlyFavorites);
            }
            public void afterTextChanged(Editable e) {}
        });
    }
    void fillStocks(LinearLayout list, String query, boolean onlyFavorites) {
        list.removeAllViews(); String q = query.trim().toLowerCase(Locale.ROOT); int count = 0;
        for (DemoBroker.Stock s : DemoBroker.STOCKS) {
            if (onlyFavorites && !a.favorites.contains(s.symbol)) continue;
            if (!(s.symbol + " " + s.name).toLowerCase(Locale.ROOT).contains(q)) continue;
            count++;
            LinearLayout line = row(); line.setPadding(0, dp(13), 0, dp(13));
            TextView avatar = text(s.symbol.substring(0, 1), 16, s.symbol.equals("NVDA") ? ACCENT : 0xffbfcae1, true);
            avatar.setGravity(Gravity.CENTER); avatar.setBackground(shape(0xff202837, 12, 0));
            line.addView(avatar, new LinearLayout.LayoutParams(dp(38), dp(38))); gap(line, 11);
            LinearLayout names = column(); names.addView(text(s.symbol, 14, TEXT, true)); space(names, 4);
            names.addView(text(s.name, 11, MUTED, false)); weighted(line, names);
            ChartView spark = new ChartView(a, s.symbol.hashCode(), s.change >= 0 ? GREEN : RED, true);
            line.addView(spark, new LinearLayout.LayoutParams(dp(49), dp(30))); gap(line, 12);
            LinearLayout values = column(); values.setGravity(Gravity.END);
            values.addView(text(money(s.cents).substring(1), 14, TEXT, true)); space(values, 4);
            values.addView(text(String.format(Locale.US, "%+.2f%%", s.change), 11, s.change >= 0 ? GREEN : RED, true));
            line.addView(values, new LinearLayout.LayoutParams(dp(72), -2));
            line.setOnClickListener(v -> a.openStock(s.symbol)); line.setFocusable(true);
            list.addView(line); divider(list);
        }
        if (count == 0) { space(list, 24); list.addView(text("No symbols found", 17, TEXT, true)); space(list, 8);
            list.addView(text(onlyFavorites ? "Add stocks using the star on a stock page, or try another search." : "Try a symbol like NVDA or a company name.", 12, MUTED, false)); }
    }
}
