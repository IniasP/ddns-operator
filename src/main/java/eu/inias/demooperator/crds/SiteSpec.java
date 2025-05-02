package eu.inias.demooperator.crds;

public record SiteSpec(
        String cloudflareRecordRef,
        String indexTemplate,
        String pageTemplate
) {
}
