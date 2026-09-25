package com.nbyeon.papertrade;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;

final class DemoStore {
    static DemoBroker load(Context context) {
        String raw = context.getSharedPreferences("papertrade", Context.MODE_PRIVATE).getString("account", null);
        DemoBroker broker = new DemoBroker();
        if (raw == null) return broker;
        try {
            JSONObject data = new JSONObject(raw);
            broker.cash = data.getLong("cash");
            broker.positions.clear();
            JSONArray positions = data.getJSONArray("positions");
            for (int i = 0; i < positions.length(); i++) {
                JSONObject p = positions.getJSONObject(i);
                broker.positions.put(p.getString("symbol"), new DemoBroker.Position(p.getInt("quantity"), p.getLong("cost")));
            }
            JSONArray orders = data.getJSONArray("orders");
            for (int i = 0; i < orders.length(); i++) {
                JSONObject o = orders.getJSONObject(i);
                broker.orders.add(new DemoBroker.Order(o.getString("id"), o.getString("symbol"),
                    o.getBoolean("buy"), o.getInt("quantity"), o.getLong("total"), o.getLong("time")));
            }
            return broker;
        } catch (Exception e) {
            throw new IllegalStateException("Saved demo account could not be read.", e);
        }
    }
    static boolean save(Context context, DemoBroker broker) {
        try {
            JSONObject data = new JSONObject();
            data.put("cash", broker.cash);
            JSONArray positions = new JSONArray();
            for (Map.Entry<String, DemoBroker.Position> e : broker.positions.entrySet()) {
                JSONObject p = new JSONObject();
                p.put("symbol", e.getKey()); p.put("quantity", e.getValue().quantity); p.put("cost", e.getValue().cost);
                positions.put(p);
            }
            data.put("positions", positions);
            JSONArray orders = new JSONArray();
            for (DemoBroker.Order o : broker.orders) {
                JSONObject row = new JSONObject();
                row.put("id", o.id); row.put("symbol", o.symbol); row.put("buy", o.buy);
                row.put("quantity", o.quantity); row.put("total", o.total); row.put("time", o.time);
                orders.put(row);
            }
            data.put("orders", orders);
            SharedPreferences prefs = context.getSharedPreferences("papertrade", Context.MODE_PRIVATE);
            return prefs.edit().putString("account", data.toString()).commit();
        } catch (Exception e) { return false; }
    }
}

