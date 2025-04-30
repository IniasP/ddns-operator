package eu.inias.demooperator.crds;

import java.time.Instant;

public record CloudflareRecordStatus(
        Long observedGeneration,
        String id,
        String lastSyncedIp,
        Instant lastUpdateTime,
        String host
) {
}
