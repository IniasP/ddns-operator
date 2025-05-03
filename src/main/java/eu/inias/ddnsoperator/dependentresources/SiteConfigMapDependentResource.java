package eu.inias.ddnsoperator.dependentresources;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import eu.inias.ddnsoperator.crds.page.PageCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteCustomResource;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteConfigMapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, SiteCustomResource> {
    private static final Parser PARSER = Parser.builder().build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder().build();

    public SiteConfigMapDependentResource() {
        super(ConfigMap.class);
    }

    @Override
    protected ConfigMap desired(SiteCustomResource site, Context<SiteCustomResource> context) {
        List<PageCustomResource> pages = getPageResources(site, context);
        Map<String, String> htmlFiles = new HashMap<>();
        htmlFiles.put("index.html", generateIndexHtml(pages, site.getSpec().indexTemplate()));
        for (PageCustomResource page : pages) {
            String path = page.getSpec().path() + ".html";
            htmlFiles.put(path, renderPage(page, site.getSpec().pageTemplate()));
        }
        return new ConfigMapBuilder()
                .withNewMetadata()
                .withName(site.getMetadata().getName())
                .withNamespace(site.getMetadata().getNamespace())
                .endMetadata()
                .withData(htmlFiles)
                .build();
    }

    private static List<PageCustomResource> getPageResources(
            SiteCustomResource site,
            Context<SiteCustomResource> context
    ) {
        return context.getClient()
                .resources(PageCustomResource.class)
                .inNamespace(site.getMetadata().getNamespace())
                .list()
                .getItems()
                .stream()
                .filter(page -> site.getMetadata().getName().equals(page.getSpec().siteRef()))
                .sorted(Comparator.comparing(p -> p.getSpec().title()))
                .toList();
    }

    private String renderPage(PageCustomResource page, String template) {
        String htmlBody = RENDERER.render(PARSER.parse(page.getSpec().content()));
        if (template == null) {
            return htmlBody;
        }
        String title = page.getSpec().title();
        return template
                .replace("{{title}}", title)
                .replace("{{content}}", htmlBody);
    }

    private String generateIndexHtml(List<PageCustomResource> pages, String template) {
        if (template == null) {
            template = "<h1>Index</h1>\n{{index}}";
        }
        String indexList = generateIndexListHtml(pages);
        return template.replace("{{index}}", indexList);
    }

    private String generateIndexListHtml(List<PageCustomResource> pages) {
        StringBuilder sb = new StringBuilder("<ul>");
        for (PageCustomResource page : pages) {
            sb.append("<li><a href=\"")
                    .append(page.getSpec().path())
                    .append(".html\">")
                    .append(page.getSpec().title())
                    .append("</a></li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }
}
