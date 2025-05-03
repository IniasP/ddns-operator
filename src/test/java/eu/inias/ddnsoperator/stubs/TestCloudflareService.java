package eu.inias.ddnsoperator.stubs;

import eu.inias.ddnsoperator.model.cloudflare.CloudflareApiRecord;
import eu.inias.ddnsoperator.model.cloudflare.CloudflareApiZone;
import eu.inias.ddnsoperator.services.CloudflareService;

import java.util.*;

public class TestCloudflareService extends CloudflareService {
    public TestCloudflareService() {
        super(null);
    }

    public Set<CloudflareApiZone> zones = new HashSet<>();
    public Map<String, Set<CloudflareApiRecord>> recordsByZoneId = new HashMap<>();

    public void addZone(CloudflareApiZone zone) {
        zones.add(zone);
        recordsByZoneId.put(zone.id(), new HashSet<>());
    }

    @Override
    public Optional<CloudflareApiZone> getZoneByName(String name) {
        return zones.stream()
                .filter(z -> z.name().equals(name))
                .findFirst();
    }

    @Override
    public CloudflareApiZone getZoneById(String zoneId) {
        return zones.stream()
                .filter(z -> z.id().equals(zoneId))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public Optional<CloudflareApiRecord> getDnsRecordById(String zoneId, String recordId) {
        return recordsByZoneId.get(zoneId).stream()
                .filter(r -> r.id().equals(recordId))
                .findFirst();
    }

    @Override
    public Optional<CloudflareApiRecord> getDnsRecordByName(String zoneId, String recordName) {
        return recordsByZoneId.get(zoneId).stream()
                .filter(r -> r.name().equals(recordName))
                .findFirst();
    }

    @Override
    public CloudflareApiRecord updateDnsRecord(String zoneId, CloudflareApiRecord record) {
        Set<CloudflareApiRecord> records = recordsByZoneId.get(zoneId);
        records.removeIf(r -> r.id().equals(record.id()));
        records.add(record);
        return record;
    }

    @Override
    public CloudflareApiRecord createDnsRecord(String zoneId, CloudflareApiRecord record) {
        CloudflareApiRecord recordWithId = new CloudflareApiRecord(
                UUID.randomUUID().toString(),
                record.name(),
                record.type(),
                record.content()
        );
        recordsByZoneId.get(zoneId).add(recordWithId);
        return recordWithId;
    }

    @Override
    public void deleteDnsRecord(String zoneId, String recordId) {
        recordsByZoneId.get(zoneId).removeIf(z -> z.id().equals(zoneId));
    }
}
