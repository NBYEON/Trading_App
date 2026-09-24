package com.nbyeon.papertrade;

/** Boundary between Android screens and account storage; tests can supply a fake. */
interface TradingGateway {
    interface Callback<T> {
        void success(T value);
        void failure(String message);
    }
    void load(Callback<DemoBroker> callback);
    void submit(String id, String symbol, boolean buy, int quantity, Callback<DemoBroker.Order> callback);
}
