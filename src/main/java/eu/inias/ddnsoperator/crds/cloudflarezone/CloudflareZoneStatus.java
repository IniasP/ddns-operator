package eu.inias.ddnsoperator.crds.cloudflarezone;

public record CloudflareZoneStatus(
        Long observedGeneration,
        String id
) {
}
