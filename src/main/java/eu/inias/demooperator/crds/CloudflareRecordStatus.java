package eu.inias.demooperator.crds;

import java.time.Instant;

public record CloudflareRecordStatus(
        Long observedGeneration,
        Boolean ready,
        String lastSyncedIp,
        Instant lastUpdateTime
) implements Status {
}
