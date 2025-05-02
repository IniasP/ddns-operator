package eu.inias.demooperator.crds.page;

public record PageSpec(
        String siteRef,
        String path,
        String title,
        String content
) {
}
