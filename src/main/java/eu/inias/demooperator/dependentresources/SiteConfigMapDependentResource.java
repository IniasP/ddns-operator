package eu.inias.demooperator.dependentresources;

import eu.inias.demooperator.crds.PageCustomResource;
import eu.inias.demooperator.crds.SiteCustomResource;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteConfigMapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, SiteCustomResource> {
    public SiteConfigMapDependentResource() {
        super(ConfigMap.class);
    }

    @Override
    protected ConfigMap desired(SiteCustomResource site, Context<SiteCustomResource> context) {
        List<PageCustomResource> pages = context.getClient()
                .resources(PageCustomResource.class)
                .inNamespace(site.getMetadata().getNamespace())
                .list()
                .getItems()
                .stream()
                .filter(page -> site.getMetadata().getName().equals(page.getSpec().siteRef()))
                .toList();

        Map<String, String> htmlFiles = new HashMap<>();
        htmlFiles.put("index.html", generateIndexHtml(pages));
        for (PageCustomResource page : pages) {
            String path = page.getSpec().path() + ".html";
            htmlFiles.put(path, page.getSpec().content());
        }
        return new ConfigMapBuilder()
                .withNewMetadata()
                .withName("site-" + site.getMetadata().getName())
                .withNamespace(site.getMetadata().getNamespace())
                .endMetadata()
                .withData(htmlFiles)
                .build();
    }

    private String generateIndexHtml(List<PageCustomResource> pages) {
        StringBuilder sb = new StringBuilder("<h1>Index</h1><ul>");
        for (PageCustomResource page : pages) {
            sb.append("<li><a href=\"")
                    .append(page.getSpec().path())
                    .append(".html\">")
                    .append(page.getMetadata().getName())
                    .append("</a></li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }
}
