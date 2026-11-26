package io.qa.ui.testdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class RestOrdersBackdoor implements OrdersBackdoor {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final String apiBaseUrl;

    public RestOrdersBackdoor(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    @Override
    public OrderSeed create(OrderSeed seed) {
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/test-data/orders"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(seed), StandardCharsets.UTF_8))
                .build());
        verifySuccess(response, "создание заказа " + seed.number());
        return fromJson(response.body());
    }

    @Override
    public List<OrderSeed> createAll(List<OrderSeed> seeds) {
        List<OrderSeed> created = new ArrayList<>(seeds.size());
        for (OrderSeed seed : seeds) {
            created.add(create(seed));
        }
        return created;
    }

    @Override
    public void deleteByCustomer(String customerPrefix) {
        String query = URLEncoder.encode(customerPrefix, StandardCharsets.UTF_8);
        HttpResponse<String> response = send(HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/test-data/orders?customerPrefix=" + query))
                .DELETE()
                .build());
        verifySuccess(response, "удаление заказов по префиксу " + customerPrefix);
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Backdoor-запрос не выполнен: " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Backdoor-запрос прерван: " + request.uri(), e);
        }
    }

    private void verifySuccess(HttpResponse<String> response, String operation) {
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Backdoor: " + operation + " завершилось с кодом "
                    + response.statusCode() + ", тело ответа: " + response.body());
        }
    }

    private String toJson(OrderSeed seed) {
        try {
            return mapper.writeValueAsString(seed);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сериализовать OrderSeed", e);
        }
    }

    private OrderSeed fromJson(String body) {
        try {
            return mapper.readValue(body, OrderSeed.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось разобрать ответ backdoor-API: " + body, e);
        }
    }
}
