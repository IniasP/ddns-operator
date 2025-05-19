package eu.inias.ddnsoperator.crds.site;

import io.fabric8.generator.annotation.Required;

public record SiteSpec(
        @Required String cloudflareRecordRef,
        String indexTemplate,
        String pageTemplate
) {
}
