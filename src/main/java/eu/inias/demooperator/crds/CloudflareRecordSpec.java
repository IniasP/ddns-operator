package eu.inias.demooperator.crds;

public record CloudflareRecordSpec(
        String zoneRef,
        String name
) {
}
