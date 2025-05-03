package eu.inias.ddnsoperator.crds.cloudflarerecord;

public record CloudflareRecordSpec(
        String zoneRef,
        String name
) {
}
