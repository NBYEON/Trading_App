package com.nbyeon.papertrade;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import static com.nbyeon.papertrade.MainActivity.*;

/** Shared native-view styling used by each screen. */
class UiKit {
    protected final MainActivity a;
    UiKit(MainActivity activity) { this.a = activity; }
    protected String money(long cents) { return MainActivity.money(cents); }
    protected int dp(float value) { return Math.round(value * a.getResources().getDisplayMetrics().density); }
    protected GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color); drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }
    protected LinearLayout column() {
        LinearLayout v = new LinearLayout(a); v.setOrientation(LinearLayout.VERTICAL); return v;
    }
    protected LinearLayout row() {
        LinearLayout v = new LinearLayout(a); v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }
    protected TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(a); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setFontFeatureSettings("tnum");
        if (bold) v.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        return v;
    }
    protected void space(LinearLayout parent, int height) {
        parent.addView(new View(a), new LinearLayout.LayoutParams(1, dp(height)));
    }
    protected void divider(LinearLayout parent) {
        View line = new View(a); line.setBackgroundColor(BORDER);
        parent.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
    }
    protected void weighted(LinearLayout parent, View child) {
        parent.addView(child, new LinearLayout.LayoutParams(0, -2, 1));
    }
    protected TextView button(String label, int fill, int color, Runnable action) {
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
    protected void gap(LinearLayout parent, int width) {
        parent.addView(new View(a), new LinearLayout.LayoutParams(dp(width), 1));
    }
    protected void title(String name, String subtitle) {
        LinearLayout header = row();
        LinearLayout labels = column();
        labels.addView(text(name, 29, TEXT, true));
        if (subtitle != null) { space(labels, 5); labels.addView(text(subtitle, 12, MUTED, false)); }
        weighted(header, labels);
        TextView badge = text("PAPER", 10, ACCENT, true);
        badge.setPadding(dp(10), dp(7), dp(10), dp(7)); badge.setBackground(shape(0xff253021, 7, 0));
        badge.setContentDescription("Paper trading demo");
        header.addView(badge); a.body.addView(header); space(a.body, 25);
    }
    protected void heading(String label, String caption) {
        LinearLayout line = row(); weighted(line, text(label, 19, TEXT, true));
        if (caption != null) line.addView(text(caption, 11, MUTED, false));
        a.body.addView(line); space(a.body, 15);
    }
    protected LinearLayout card() {
        LinearLayout v = column(); v.setPadding(dp(16), dp(16), dp(16), dp(16));
        v.setBackground(shape(SURFACE, 15, BORDER)); return v;
    }
    protected void dataRow(LinearLayout parent, String label, String value) {
        LinearLayout line = row(); line.setPadding(0, dp(9), 0, dp(9));
        weighted(line, text(label, 12, MUTED, false)); line.addView(text(value, 12, TEXT, true)); parent.addView(line);
    }
}
