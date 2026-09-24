package com.nbyeon.papertrade;

import android.app.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import static com.nbyeon.papertrade.MainActivity.*;

/** Validates and submits a reviewed paper order through the server gateway. */
final class OrderTicket extends UiKit {
    OrderTicket(MainActivity activity) { super(activity); }
    /** Reviews input locally; only the confirmed request can change server state. */
    void showOrder(DemoBroker.Stock stock, boolean buy) {
        if (!a.online) {
            new AlertDialog.Builder(a).setTitle("Connect to demo server")
                .setMessage("Orders require the Spring Boot server. Start it and tap Retry.")
                .setPositiveButton("Retry", (dialog, which) -> a.refreshRemote())
                .setNegativeButton("Close", null).show();
            return;
        }
        a.orderDialog = new Dialog(a); a.orderDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll = new ScrollView(a); LinearLayout form = column();
        form.setPadding(dp(24), dp(24), dp(24), dp(24)); form.setBackground(shape(SURFACE, 22, BORDER)); scroll.addView(form);
        form.addView(text("PAPER ORDER", 10, ACCENT, true)); space(form, 14);
        form.addView(text((buy ? "Buy " : "Sell ") + stock.symbol, 27, TEXT, true)); space(form, 6);
        form.addView(text(stock.name + " · Market order", 13, MUTED, false)); space(form, 20);
        dataRow(form, "Price per share", money(stock.cents));
        dataRow(form, buy ? "Buying power" : "Available shares", buy ? money(a.broker.cash) :
            (a.broker.positions.containsKey(stock.symbol) ? a.broker.positions.get(stock.symbol).quantity : 0) + " shares");
        space(form, 18); form.addView(text("Quantity", 12, MUTED, true)); space(form, 8);
        EditText quantity = new EditText(a); quantity.setSingleLine(true);
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
            if (buy && stock.cents * n > a.broker.cash) { error.setText("Not enough buying power. Try fewer shares."); return; }
            if (!buy && (!a.broker.positions.containsKey(stock.symbol) || a.broker.positions.get(stock.symbol).quantity < n)) {
                error.setText("Not enough shares to sell."); return;
            }
            new AlertDialog.Builder(a).setTitle("Confirm paper " + (buy ? "buy" : "sell"))
                .setMessage(n + " shares of " + stock.symbol + " at " + money(stock.cents) +
                    "\nTotal: " + money(stock.cents * n) + "\n\nThis simulated order fills immediately. No real money is used.")
                .setNegativeButton("Go back", null)
                .setPositiveButton("Place paper order", (dialog, which) -> {
                    error.setText("Submitting paper order…");
                    a.gateway.submit(requestId, stock.symbol, buy, n, new TradingGateway.Callback<DemoBroker.Order>() {
                        public void success(DemoBroker.Order filled) {
                            if (a.isDestroyed()) return;
                            a.orderDialog.dismiss(); a.selected = null; a.section = "Orders";
                            // Keep the acknowledged fill visible while fetching the full server snapshot.
                            boolean alreadyShown = false;
                            for (DemoBroker.Order prior : a.broker.orders)
                                if (prior.id.equals(filled.id)) { alreadyShown = true; break; }
                            if (!alreadyShown) a.broker.orders.add(0, filled);
                            DemoStore.save(a, a.broker);
                            a.render();
                            a.refreshRemote();
                            Toast.makeText(a, "Paper order filled", Toast.LENGTH_SHORT).show();
                        }
                        public void failure(String message) { if (!a.isDestroyed()) error.setText(message); }
                    });
                }).show();
        }));
        space(form, 10); form.addView(button("Cancel", SURFACE, MUTED, () -> a.orderDialog.dismiss()));
        a.orderDialog.setContentView(scroll); a.orderDialog.show();
        Window window = a.orderDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(a.getResources().getDisplayMetrics().widthPixels - dp(24), ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }
}
