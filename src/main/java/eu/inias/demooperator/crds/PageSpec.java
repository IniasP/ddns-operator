package eu.inias.demooperator.crds;

public record PageSpec(
        String siteRef,
        String path,
        String title,
        String content
) {
}
