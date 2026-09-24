package com.nbyeon.papertrade.api;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

/** Wire shapes shared conceptually with the Android client. Money is always integer cents. */
public final class Models {
    private Models() {}
    public record Stock(String symbol, String name, String sector, long cents, double change) {}
    public record Position(String symbol, int quantity, long cost) {}
    public record Order(UUID id, String symbol, boolean buy, int quantity, long total, long time) {}
    public record Account(long cash, List<Position> positions, List<Order> orders) {}
    public record SubmitOrder(@NotNull UUID id, @NotBlank String symbol, @NotNull Boolean buy,
                              @NotNull @Min(1) @Max(1000000) Integer quantity) {}
    public record ApiError(String code, String message) {}
}
