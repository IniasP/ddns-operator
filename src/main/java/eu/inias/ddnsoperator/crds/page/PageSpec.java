package eu.inias.ddnsoperator.crds.page;

public record PageSpec(
        String siteRef,
        String path,
        String title,
        String content
) {
}
