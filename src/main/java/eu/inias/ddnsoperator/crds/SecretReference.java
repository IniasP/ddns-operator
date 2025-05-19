package eu.inias.ddnsoperator.crds;

import io.fabric8.generator.annotation.Required;

public record SecretReference(
        @Required String name,
        @Required String key
) {
}
