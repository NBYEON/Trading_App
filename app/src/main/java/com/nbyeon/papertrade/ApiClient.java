package com.nbyeon.papertrade;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Runs HTTP off the UI thread and returns results to Android's main thread. */
final class ApiClient implements TradingGateway {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final String baseUrl;
    ApiClient(String baseUrl) { this.baseUrl = baseUrl; }
    void close() { executor.shutdownNow(); }

    /** Fetches quotes and account state off the UI thread, then delivers a snapshot. */
    @Override public void load(Callback<DemoBroker> callback) {
        executor.execute(() -> {
            try {
                JSONArray stocks = new JSONArray(request("GET", "/stocks", null));
                JSONObject account = new JSONObject(request("GET", "/account", null));
                DemoBroker snapshot = new DemoBroker();
                snapshot.cash = account.getLong("cash");
                snapshot.positions.clear(); snapshot.orders.clear();
                for (int i = 0; i < stocks.length(); i++) {
                    JSONObject s = stocks.getJSONObject(i);
                    DemoBroker.STOCKS[i] = new DemoBroker.Stock(s.getString("symbol"), s.getString("name"),
                        s.getString("sector"), s.getLong("cents"), s.getDouble("change"));
                }
                JSONArray positions = account.getJSONArray("positions");
                for (int i = 0; i < positions.length(); i++) {
                    JSONObject p = positions.getJSONObject(i);
                    snapshot.positions.put(p.getString("symbol"), new DemoBroker.Position(p.getInt("quantity"), p.getLong("cost")));
                }
                JSONArray orders = account.getJSONArray("orders");
                for (int i = 0; i < orders.length(); i++) {
                    JSONObject o = orders.getJSONObject(i);
                    snapshot.orders.add(new DemoBroker.Order(o.getString("id"), o.getString("symbol"),
                        o.getBoolean("buy"), o.getInt("quantity"), o.getLong("total"), o.getLong("time")));
                }
                main.post(() -> callback.success(snapshot));
            } catch (Exception e) { main.post(() -> callback.failure(e.getMessage())); }
        });
    }

    /** Sends the stable request ID to let the server recognize a retry. */
    @Override public void submit(String id, String symbol, boolean buy, int quantity, Callback<DemoBroker.Order> callback) {
        executor.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", id); payload.put("symbol", symbol);
                payload.put("buy", buy); payload.put("quantity", quantity);
                JSONObject result = new JSONObject(request("POST", "/orders", payload.toString()));
                DemoBroker.Order order = new DemoBroker.Order(result.getString("id"), result.getString("symbol"),
                    result.getBoolean("buy"), result.getInt("quantity"), result.getLong("total"), result.getLong("time"));
                main.post(() -> callback.success(order));
            } catch (Exception e) { main.post(() -> callback.failure(e.getMessage())); }
        });
    }

    private String request(String method, String path, String body) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(4000); connection.setReadTimeout(4000);
        connection.setRequestProperty("Accept", "application/json");
        if (body != null) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            try (OutputStream out = connection.getOutputStream()) { out.write(body.getBytes(StandardCharsets.UTF_8)); }
        }
        try {
            int status = connection.getResponseCode();
            InputStream stream = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) throw new IOException("Server returned HTTP " + status);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            String response = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            if (status >= 400) {
                try { throw new IOException(new JSONObject(response).getString("message")); }
                catch (org.json.JSONException invalid) { throw new IOException("Server returned HTTP " + status); }
            }
            return response;
        } finally { connection.disconnect(); }
    }
}
