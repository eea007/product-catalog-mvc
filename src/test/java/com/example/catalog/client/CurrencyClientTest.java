package com.example.catalog.client;

import com.example.catalog.model.CurrencyConversion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CurrencyClientTest {

    private static final String BASE_URL = "http://currency.test";
    private static final String CONVERT_URL = BASE_URL + "/api/currencies/convert?from=USD&to=eur&amount=99.99";
    private static final BigDecimal AMOUNT = new BigDecimal("99.99");

    private MockRestServiceServer server;
    private CurrencyClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new CurrencyClient(builder.build());
    }

    @Test
    void convert_success_returnsConvertedAmountWithConvertedTrue() {
        server.expect(requestTo(CONVERT_URL))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {"fromCurrency":"USD","toCurrency":"EUR","originalAmount":99.99,
                 "convertedAmount":91.50,"exchangeRate":0.915}
                """, MediaType.APPLICATION_JSON));

        CurrencyConversion result = client.convert(AMOUNT, "eur");

        assertThat(result.amount()).isEqualByComparingTo("91.50");
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.converted()).isTrue();
        server.verify();
    }

    @Test
    void convert_serverError_fallsBackToUsdWithConvertedFalse() {
        server.expect(requestTo(CONVERT_URL)).andRespond(withServerError());

        assertThat(client.convert(AMOUNT, "eur")).isEqualTo(new CurrencyConversion(AMOUNT, "USD", false));
    }

    @Test
    void convert_timeout_fallsBackToUsdWithConvertedFalse() {
        server.expect(requestTo(CONVERT_URL))
            .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThat(client.convert(AMOUNT, "eur")).isEqualTo(new CurrencyConversion(AMOUNT, "USD", false));
    }

    @Test
    void convert_emptyBody_fallsBackToUsdWithConvertedFalse() {
        server.expect(requestTo(CONVERT_URL)).andRespond(withSuccess());

        assertThat(client.convert(AMOUNT, "eur")).isEqualTo(new CurrencyConversion(AMOUNT, "USD", false));
    }
}
