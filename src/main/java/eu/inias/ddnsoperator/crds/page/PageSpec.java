package eu.inias.ddnsoperator.crds.page;

import io.fabric8.generator.annotation.Required;

public record PageSpec(
        @Required String siteRef,
        @Required String path,
        @Required String title,
        @Required String content
) {
}
