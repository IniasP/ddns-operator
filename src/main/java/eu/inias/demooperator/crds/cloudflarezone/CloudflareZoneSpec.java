package eu.inias.demooperator.crds.cloudflarezone;

import eu.inias.demooperator.crds.SecretReference;

public record CloudflareZoneSpec(
        String domain,
        SecretReference apiTokenSecretRef
) {
}
