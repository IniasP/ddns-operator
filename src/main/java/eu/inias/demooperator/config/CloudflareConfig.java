package eu.inias.demooperator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CloudflareConfig {
    @Bean
    public RestClient cloudflareRestClient(
            @Value("${cloudflare.api.base-url}") String baseUrl,
            @Value("${cloudflare.api.bearer}") String bearerToken
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor((req, body, execution) -> {
                    req.getHeaders().setBearerAuth(bearerToken);
                    return execution.execute(req, body);
                })
                .build();
    }
}
