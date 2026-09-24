package com.nbyeon.papertrade.api;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import static com.nbyeon.papertrade.api.Models.*;

@RestController
@RequestMapping("/api")
public final class TradingController {
    private final TradingService service;
    TradingController(TradingService service) { this.service = service; }
    @GetMapping("/stocks") public List<Stock> stocks() { return service.stocks(); }
    @GetMapping("/account") public Account account() { return service.account(); }
    @PostMapping("/orders") public Order submit(@Valid @RequestBody SubmitOrder request) { return service.submit(request); }
}
