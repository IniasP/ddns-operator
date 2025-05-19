package eu.inias.ddnsoperator.crds.cloudflarezone;

import eu.inias.ddnsoperator.crds.SecretReference;
import io.fabric8.generator.annotation.Required;

public record CloudflareZoneSpec(
        @Required String domain,
        @Required SecretReference apiTokenSecretRef
) {
}
