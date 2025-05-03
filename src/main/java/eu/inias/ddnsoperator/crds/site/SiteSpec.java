package eu.inias.ddnsoperator.crds.site;

public record SiteSpec(
        String cloudflareRecordRef,
        String indexTemplate,
        String pageTemplate
) {
}
