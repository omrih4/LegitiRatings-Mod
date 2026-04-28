package me.omrih.legitiratings.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GETManager {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static GETManager INSTANCE;
    // 5 Minutes
    private final long cacheTTL = 300000L;
    private long lastFetched;

    private String reviewsResponse;

    /**
     * Returns the cached reviews or fetches <code>/review/</code>
     */
    public CompletableFuture<JsonArray> get() {
        if (reviewsResponse == null || System.currentTimeMillis() - lastFetched > cacheTTL) {
            return refreshCache();
        }

        return CompletableFuture.completedFuture(JsonParser.parseString(reviewsResponse).getAsJsonArray());
    }

    public CompletableFuture<JsonArray> refreshCache() {
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("https://ratings.legiti.dev/review/"))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        return httpClient.sendAsync(get, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (response.statusCode() == 200) {
                reviewsResponse = response.body();
                lastFetched = System.currentTimeMillis();
            }
            return JsonParser.parseString(reviewsResponse).getAsJsonArray();
        });
    }

    public static GETManager getInstance() {
        if (INSTANCE == null) INSTANCE = new GETManager();
        return INSTANCE;
    }
}
