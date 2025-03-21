package Api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class ApiClient {
    private final HttpClient client;

    public ApiClient() {
        this.client = HttpClient.newHttpClient();
    }

    public Map<String, String> fetchAllData() throws Exception {
        Map<String, String> data = new HashMap<>();
        String[] urls = {
                "https://fetch.theglobalfund.org/v4.2/odata/Grants",
                "https://fetch.theglobalfund.org/v4.2/odata/FundingRequests" // Dropped FinancialIndicators
        };

        for (String url : urls) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().startsWith("{")) {
                data.put(url, response.body());
                System.out.println("Data loaded successfully from " + url);
                if (url.contains("Grants")) {
                    System.out.println("Full Grants response (snippet): " +
                            response.body().substring(0, Math.min(2000, response.body().length())));
                }
            } else {
                System.err.println("Failed to load from " + url + ": Status " + response.statusCode());
            }
        }
        return data;
    }
}
