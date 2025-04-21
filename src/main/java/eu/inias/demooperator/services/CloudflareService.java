package eu.inias.demooperator.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CloudflareService {
    private final RestClient cloudflareRestClient;

    public CloudflareService(RestClient cloudflareRestClient) {
        this.cloudflareRestClient = cloudflareRestClient;
    }

    public String getZoneId() {
        return cloudflareRestClient.get()
                .uri("/zones")
                .retrieve()
                .body(String.class);
    }
}
