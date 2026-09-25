package com.nbyeon.papertrade;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DemoBrokerTest {
    @Test public void buyAndSellUpdateCashAndHoldings() {
        DemoBroker b = new DemoBroker(); long cash = b.cash;
        b.execute("buy", "NVDA", true, 2);
        assertEquals(cash - 25568, b.cash); assertEquals(14, b.positions.get("NVDA").quantity);
        b.execute("sell", "NVDA", false, 14);
        assertFalse(b.positions.containsKey("NVDA")); assertEquals(cash + 12 * 12784, b.cash);
    }
    @Test public void invalidOrdersLeaveAccountUntouched() {
        DemoBroker b = new DemoBroker(); long cash = b.cash, value = b.marketValue();
        for (Runnable action : new Runnable[] {
            () -> b.execute("a", "NVDA", true, 0),
            () -> b.execute("b", "NVDA", true, 1000000),
            () -> b.execute("c", "NVDA", false, 13),
            () -> b.execute("d", "BAD", true, 1)
        }) {
            assertThrows(IllegalArgumentException.class, action::run);
            assertEquals(cash, b.cash); assertEquals(value, b.marketValue()); assertTrue(b.orders.isEmpty());
        }
    }
    @Test public void retriesAreIdempotentAndCannotChangePayload() {
        DemoBroker b = new DemoBroker();
        b.execute("same", "AAPL", true, 1); long cash = b.cash;
        b.execute("same", "AAPL", true, 1);
        assertEquals(cash, b.cash); assertEquals(1, b.orders.size());
        assertThrows(IllegalArgumentException.class, () -> b.execute("same", "AAPL", true, 2));
    }
    @Test public void competingBuysCannotOverspend() throws Exception {
        DemoBroker b = new DemoBroker(); b.cash = DemoBroker.stock("NVDA").cents;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1); AtomicInteger accepted = new AtomicInteger();
        try {
            Future<?> a = pool.submit(() -> submit(b, "a", start, accepted));
            Future<?> c = pool.submit(() -> submit(b, "c", start, accepted));
            start.countDown(); a.get(5, TimeUnit.SECONDS); c.get(5, TimeUnit.SECONDS);
            assertEquals(1, accepted.get()); assertEquals(0, b.cash); assertEquals(1, b.orders.size());
        } finally { pool.shutdownNow(); }
    }
    private void submit(DemoBroker b, String id, CountDownLatch start, AtomicInteger accepted) {
        try { start.await(); b.execute(id, "NVDA", true, 1); accepted.incrementAndGet(); }
        catch (IllegalArgumentException expected) { /* Second order must fail. */ }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
}

