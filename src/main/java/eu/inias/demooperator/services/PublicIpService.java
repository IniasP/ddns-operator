package eu.inias.demooperator.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PublicIpService {
    private final RestClient restClient;

    public PublicIpService(RestClient restClient) {
        this.restClient = restClient;
    }

    public String getPublicIp() {
        String ip = restClient.get()
                .uri("https://icanhazip.com")
                .retrieve()
                .body(String.class);
        if (ip == null) {
            throw new IllegalStateException("Failed to obtain public ip.");
        }
        return ip.strip();
    }
}
