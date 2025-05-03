package eu.inias.ddnsoperator.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CloudflareServiceFactory {
    private final String baseUrl;

    public CloudflareServiceFactory(@Value("${cloudflare.api.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public CloudflareService create(String bearerToken) {
        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((req, body, execution) -> {
                    req.getHeaders().setBearerAuth(bearerToken);
                    return execution.execute(req, body);
                })
                .build();
        return new CloudflareService(client);
    }
}
