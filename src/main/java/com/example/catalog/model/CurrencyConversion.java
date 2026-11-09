package com.example.catalog.model;

import java.math.BigDecimal;

/** Result of CurrencyClient.convert: the amount, the currency it is in, and whether a conversion happened. */
public record CurrencyConversion(
    BigDecimal amount,
    String currency,
    boolean converted
) {
    public static CurrencyConversion unconverted(BigDecimal usdAmount) {
        return new CurrencyConversion(usdAmount, "USD", false);
    }
}
