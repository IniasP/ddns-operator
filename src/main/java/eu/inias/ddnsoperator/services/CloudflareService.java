package eu.inias.ddnsoperator.services;

import eu.inias.ddnsoperator.model.cloudflare.CloudflareApiRecord;
import eu.inias.ddnsoperator.model.cloudflare.CloudflareApiResponse;
import eu.inias.ddnsoperator.model.cloudflare.CloudflareApiZone;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class CloudflareService {
    private final RestClient cloudflareRestClient;

    public CloudflareService(RestClient restClient) {
        this.cloudflareRestClient = restClient;
    }

    public Optional<CloudflareApiZone> getZoneByName(String name) {
        return cloudflareRestClient.get()
                .uri("/zones")
                .retrieve()
                .body(new ParameterizedTypeReference<CloudflareApiResponse<List<CloudflareApiZone>>>() {
                })
                .result()
                .stream()
                .filter(z -> z.name().equals(name))
                .findFirst();
    }

    public CloudflareApiZone getZoneById(String zoneId) {
        return cloudflareRestClient.get()
                        .uri("/zones/{zoneId}", zoneId)
                        .retrieve()
                        .body(new ParameterizedTypeReference<CloudflareApiResponse<CloudflareApiZone>>() {
                        })
                        .result();
    }

    public Optional<CloudflareApiRecord> getDnsRecordById(String zoneId, String recordId) {
        return wrapNotFound(() ->
                cloudflareRestClient.get()
                        .uri("/zones/{zoneId}/dns_records/{recordId}", zoneId, recordId)
                        .retrieve()
                        .body(new ParameterizedTypeReference<CloudflareApiResponse<CloudflareApiRecord>>() {
                        })
                        .result()
        );
    }

    public Optional<CloudflareApiRecord> getDnsRecordByName(String zoneId, String recordName) {
        return cloudflareRestClient.get()
                .uri("/zones/{zoneId}/dns_records", zoneId)
                .retrieve()
                .body(new ParameterizedTypeReference<CloudflareApiResponse<List<CloudflareApiRecord>>>() {
                })
                .result()
                .stream()
                .filter(r -> r.type().equals("A") && r.name().equals(recordName))
                .findFirst();
    }

    public CloudflareApiRecord updateDnsRecord(String zoneId, CloudflareApiRecord record) {
        return cloudflareRestClient.patch()
                .uri("/zones/{zoneId}/dns_records/{recordId}", zoneId, record.id())
                .body(record)
                .retrieve()
                .body(new ParameterizedTypeReference<CloudflareApiResponse<CloudflareApiRecord>>() {
                })
                .result();
    }

    public CloudflareApiRecord createDnsRecord(String zoneId, CloudflareApiRecord record) {
        return cloudflareRestClient.post()
                .uri("/zones/{zoneId}/dns_records", zoneId)
                .body(record)
                .retrieve()
                .body(new ParameterizedTypeReference<CloudflareApiResponse<CloudflareApiRecord>>() {
                })
                .result();
    }

    public void deleteDnsRecord(String zoneId, String recordId) {
        cloudflareRestClient.delete()
                .uri("/zones/{zoneId}/dns_records/{recordId}", zoneId, recordId)
                .retrieve()
                .toBodilessEntity();
    }

    private <R> Optional<R> wrapNotFound(Supplier<R> supplier) {
        try {
            return Optional.of(supplier.get());
        } catch (NotFound e) {
            return Optional.empty();
        }
    }
}
