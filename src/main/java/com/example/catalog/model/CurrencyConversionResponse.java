package com.example.catalog.model;

import java.math.BigDecimal;

/** JSON returned by currency-service GET /api/currencies/convert. */
public record CurrencyConversionResponse(
    String fromCurrency,
    String toCurrency,
    BigDecimal originalAmount,
    BigDecimal convertedAmount,
    BigDecimal exchangeRate
) {}
