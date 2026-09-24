package com.nbyeon.papertrade.api;

import java.util.UUID;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static com.nbyeon.papertrade.api.Models.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TradingIntegrationTest {
    @Autowired TradingService service;
    @Autowired JdbcTemplate jdbc;
    @Value("${local.server.port}") int port;

    @BeforeEach void resetDemoAccount() {
        jdbc.update("DELETE FROM orders");
        jdbc.update("DELETE FROM positions");
        jdbc.update("UPDATE accounts SET cash_cents=2475000 WHERE id=1");
        jdbc.update("INSERT INTO positions VALUES (1,'NVDA',12,139200),(1,'AAPL',8,176000),(1,'MSFT',5,205000)");
    }
    @Test void buyAndSellPersistAcrossReads() {
        long startingCash = service.account().cash();
        Order buy = service.submit(new SubmitOrder(UUID.randomUUID(), "NVDA", true, 2));
        assertEquals(25568, buy.total());
        assertEquals(startingCash - 25568, service.account().cash());
        assertEquals(14, service.account().positions().stream().filter(p -> p.symbol().equals("NVDA")).findFirst().orElseThrow().quantity());
        service.submit(new SubmitOrder(UUID.randomUUID(), "NVDA", false, 14));
        assertEquals(startingCash + 12 * 12784, service.account().cash());
        assertFalse(service.account().positions().stream().anyMatch(p -> p.symbol().equals("NVDA")));
    }
    @Test void rejectionRollsBackEveryTable() {
        Account before = service.account();
        assertThrows(TradingException.class, () -> service.submit(new SubmitOrder(UUID.randomUUID(), "NVDA", true, 1_000_000)));
        assertThrows(TradingException.class, () -> service.submit(new SubmitOrder(UUID.randomUUID(), "NVDA", false, 13)));
        assertEquals(before, service.account());
    }
    @Test void sameRequestCannotFillTwiceOrChangeMeaning() {
        UUID id = UUID.randomUUID();
        Order first = service.submit(new SubmitOrder(id, "AAPL", true, 1));
        long cash = service.account().cash();
        assertEquals(first, service.submit(new SubmitOrder(id, "AAPL", true, 1)));
        assertEquals(cash, service.account().cash());
        assertEquals(1, service.account().orders().size());
        assertThrows(TradingException.class, () -> service.submit(new SubmitOrder(id, "AAPL", true, 2)));
    }
    @Test void competingOrdersCannotOverspend() throws Exception {
        jdbc.update("UPDATE accounts SET cash_cents=12784 WHERE id=1");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger filled = new AtomicInteger();
        try {
            Future<?> a = pool.submit(() -> attempt(start, filled));
            Future<?> b = pool.submit(() -> attempt(start, filled));
            start.countDown(); a.get(10, TimeUnit.SECONDS); b.get(10, TimeUnit.SECONDS);
            assertEquals(1, filled.get());
            assertEquals(0, service.account().cash());
            assertEquals(1, service.account().orders().size());
        } finally { pool.shutdownNow(); }
    }
    @Test void androidJsonContractWorksOverHttp() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        String base = "http://127.0.0.1:" + port + "/api";
        HttpResponse<String> account = client.send(HttpRequest.newBuilder(URI.create(base + "/account")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertEquals(200, account.statusCode());
        assertTrue(account.body().contains("\"cash\":2475000"));
        UUID id = UUID.randomUUID();
        String order = "{\"id\":\"" + id + "\",\"symbol\":\"NVDA\",\"buy\":true,\"quantity\":2}";
        HttpResponse<String> fill = client.send(HttpRequest.newBuilder(URI.create(base + "/orders"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(order)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, fill.statusCode());
        assertTrue(fill.body().contains("\"total\":25568"));
    }
    private void attempt(CountDownLatch start, AtomicInteger filled) {
        try {
            start.await();
            service.submit(new SubmitOrder(UUID.randomUUID(), "NVDA", true, 1));
            filled.incrementAndGet();
        } catch (TradingException expected) {
            assertEquals("INSUFFICIENT_CASH", expected.code);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw new AssertionError(e);
        }
    }
}
