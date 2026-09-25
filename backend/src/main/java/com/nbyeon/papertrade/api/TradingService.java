package com.nbyeon.papertrade.api;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.nbyeon.papertrade.api.Models.*;

@Service
public class TradingService {
    private final TradingRepository repository;
    TradingService(TradingRepository repository) { this.repository = repository; }
    public List<Stock> stocks() { return repository.stocks(); }
    public Account account() { return repository.account(); }

    /**
     * One database transaction covers the cash change, position change and fill record.
     * Locking the account row also serializes retries, so a request ID cannot fill twice.
     */
    @Transactional
    public Order submit(SubmitOrder request) {
        int quantity = request.quantity();
        long cash = repository.lockedCash();
        Order previous = repository.existing(request.id());
        if (previous != null) {
            if (!previous.symbol().equals(request.symbol()) || previous.buy() != request.buy()
                || previous.quantity() != quantity)
                throw new TradingException(HttpStatus.CONFLICT, "REQUEST_REUSED", "Request ID belongs to a different order.");
            return previous;
        }
        Stock stock = repository.stock(request.symbol());
        if (stock == null) throw new TradingException(HttpStatus.BAD_REQUEST, "UNKNOWN_SYMBOL", "Unknown stock symbol.");
        long total = Math.multiplyExact(stock.cents(), quantity);
        Position position = repository.position(stock.symbol());
        if (request.buy()) {
            if (total > cash) throw new TradingException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_CASH", "Not enough buying power.");
            int current = position == null ? 0 : position.quantity();
            long cost = position == null ? 0 : position.cost();
            repository.setCash(cash - total);
            repository.upsertPosition(stock.symbol(), Math.addExact(current, quantity), Math.addExact(cost, total));
        } else {
            if (position == null || position.quantity() < quantity)
                throw new TradingException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_SHARES", "Not enough shares to sell.");
            long removedCost = BigDecimal.valueOf(position.cost()).multiply(BigDecimal.valueOf(quantity))
                .divide(BigDecimal.valueOf(position.quantity()), 0, RoundingMode.HALF_UP).longValueExact();
            repository.setCash(Math.addExact(cash, total));
            if (position.quantity() == quantity) repository.deletePosition(stock.symbol());
            else repository.upsertPosition(stock.symbol(), position.quantity() - quantity, position.cost() - removedCost);
        }
        return repository.record(request.id(), stock.symbol(), request.buy(), quantity, total);
    }
}
