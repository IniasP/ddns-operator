package eu.inias.ddnsoperator.crds.cloudflarezone;

import eu.inias.ddnsoperator.crds.SecretReference;

public record CloudflareZoneSpec(
        String domain,
        SecretReference apiTokenSecretRef
) {
}
