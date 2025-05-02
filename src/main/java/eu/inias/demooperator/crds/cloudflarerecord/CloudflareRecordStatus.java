package eu.inias.demooperator.crds.cloudflarerecord;

import java.time.Instant;

public record CloudflareRecordStatus(
        Long observedGeneration,
        String id,
        String lastSyncedIp,
        Instant lastUpdateTime,
        String host
) {
}
