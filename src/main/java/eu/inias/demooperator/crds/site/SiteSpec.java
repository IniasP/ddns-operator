package eu.inias.demooperator.crds.site;

public record SiteSpec(
        String cloudflareRecordRef,
        String indexTemplate,
        String pageTemplate
) {
}
