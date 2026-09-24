package example.saas;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiKey;
    private final String baseUrl;

    public InfraiClient(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public String request(String method, String path, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json");
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpResponse<String> response = http.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString());
        String envelope = response.body();
        if (envelope.contains("\"ok\":false")) {
            throw new IOException("Infrai request rejected: " + envelope);
        }
        if (response.statusCode() >= 500) {
            throw new IOException("Infrai transport failure: HTTP " + response.statusCode());
        }
        return envelope;
    }
}
