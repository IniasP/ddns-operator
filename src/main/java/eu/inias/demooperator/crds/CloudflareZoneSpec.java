package eu.inias.demooperator.crds;

public record CloudflareZoneSpec(
        String domain,
        SecretReference apiTokenSecretRef
) {
}
