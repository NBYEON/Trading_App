package com.nbyeon.papertrade;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;
import java.io.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w412dp-h892dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MainActivityTest {
    private ActivityController<MainActivity> controller;
    private MainActivity activity;
    @Before public void setup() {
        RuntimeEnvironment.getApplication().getSharedPreferences("papertrade", Context.MODE_PRIVATE).edit().clear().commit();
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        activity = controller.get();
    }
    @After public void close() { controller.pause().stop().destroy(); }
    private View root() { return activity.getWindow().getDecorView(); }
    private View find(View view, String label) {
        if (view instanceof TextView && !(view instanceof EditText) && ((TextView) view).getText().toString().equals(label)) return view;
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) { View result = find(group.getChildAt(i), label); if (result != null) return result; }
        }
        return null;
    }
    private void click(String label) {
        View v = find(root(), label); assertNotNull(label, v);
        while (!v.isClickable() && v.getParent() instanceof View) v = (View) v.getParent();
        assertTrue(label, v.performClick()); shadowOf(Looper.getMainLooper()).idle();
    }
    private void screenshot(String name) throws IOException {
        shadowOf(Looper.getMainLooper()).idle();
        View view = root();
        view.measure(View.MeasureSpec.makeMeasureSpec(412, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(892, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 412, 892);
        Bitmap bitmap = Bitmap.createBitmap(412, 892, Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        File dir = new File("build/screenshots"); assertTrue(dir.isDirectory() || dir.mkdirs());
        try (OutputStream out = new FileOutputStream(new File(dir, name + ".png"))) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
        bitmap.recycle();
    }
    @Test public void screensAndSearch() throws Exception {
        screenshot("01-markets");
        EditText search = (EditText) find(root(), "Search symbol or company");
        search.setText("zzzz"); assertNotNull(find(root(), "No symbols found"));
        search.setText("NVDA"); assertNotNull(find(root(), "NVDA")); assertNull(find(root(), "AAPL"));
        click("NVDA"); assertNotNull(find(root(), "Buy NVDA")); screenshot("02-stock");
        click("1W"); click("Line ▾"); assertNotNull(find(root(), "Candles ▾"));
        click("Watchlist tab"); screenshot("03-watchlist");
        click("Portfolio tab"); screenshot("04-portfolio");
        click("Orders tab"); assertNotNull(find(root(), "Your first move awaits")); screenshot("05-orders-empty");
    }
    @Test public void orderFlowPersistsAndRejectsInvalidQuantity() throws Exception {
        click("NVDA"); click("Buy NVDA");
        Dialog ticket = ShadowDialog.getLatestDialog();
        View content = ticket.getWindow().getDecorView();
        EditText quantity = (EditText) find(content, "Order quantity");
        quantity.setText("999999");
        find(content, "Review order").performClick();
        assertNotNull(find(content, "Not enough buying power. Try fewer shares."));
        quantity.setText("2"); find(content, "Review order").performClick();
        AlertDialog confirm = ShadowAlertDialog.getLatestAlertDialog();
        confirm.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        shadowOf(Looper.getMainLooper()).idle();
        assertNotNull(find(root(), "Buy NVDA")); assertNotNull(find(root(), "FILLED"));
        DemoBroker saved = DemoStore.load(activity);
        assertEquals(14, saved.positions.get("NVDA").quantity); assertEquals(1, saved.orders.size());
        screenshot("06-order-filled");
        controller.recreate(); activity = controller.get();
        assertNotNull(find(root(), "FILLED"));
    }
    @Test public void stockSelectionSurvivesRecreation() {
        click("TSLA"); click("1M");
        controller.recreate(); activity = controller.get();
        assertNotNull(find(root(), "Buy TSLA"));
    }
}

