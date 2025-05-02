package eu.inias.demooperator.crds.cloudflarerecord;

public record CloudflareRecordSpec(
        String zoneRef,
        String name
) {
}
