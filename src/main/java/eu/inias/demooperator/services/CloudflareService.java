package eu.inias.demooperator.services;

import eu.inias.demooperator.model.cloudflare.CloudflareApiRecord;
import eu.inias.demooperator.model.cloudflare.CloudflareApiResponse;
import eu.inias.demooperator.model.cloudflare.CloudflareApiZone;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.Optional;

public class CloudflareService {
    private final RestClient cloudflareRestClient;

    public CloudflareService(RestClient restClient) {
        this.cloudflareRestClient = restClient;
    }

    public CloudflareApiZone getZone(String name) {
        return cloudflareRestClient.get()
                .uri("/zones")
                .retrieve()
                .body(new ParameterizedTypeReference<CloudflareApiResponse<CloudflareApiZone>>() {
                })
                .result()
                .stream()
                .filter(z -> z.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Zone %s not found.".formatted(name)));
    }

    public Optional<CloudflareApiRecord> getDnsRecord(String zoneId, String recordName) {
        return cloudflareRestClient.get()
                .uri("/zones/{zoneId}/dns_records", zoneId)
                .retrieve()
                .body(new ParameterizedTypeReference<CloudflareApiResponse<CloudflareApiRecord>>() {
                })
                .result()
                .stream()
                .filter(r -> r.name().equals(recordName))
                .findFirst();
    }

    public void updateDnsRecord(String zoneId, CloudflareApiRecord record) {
        cloudflareRestClient.patch()
                .uri("/zones/{zoneId}/dns_records/{recordId}", zoneId, record.id())
                .body(record)
                .retrieve()
                .toBodilessEntity();
    }

    public void createDnsRecord(String zoneId, CloudflareApiRecord record) {
        cloudflareRestClient.post()
                .uri("/zones/{zoneId}/dns_records", zoneId)
                .body(record)
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteDnsRecord(String zoneId, String recordId) {
        cloudflareRestClient.delete()
                .uri("/zones/{zoneId}/dns_records/{recordId}", zoneId, recordId)
                .retrieve()
                .toBodilessEntity();
    }
}
