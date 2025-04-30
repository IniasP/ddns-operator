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
            String content = wrapPageContent(page.getMetadata().getName(), page.getSpec().content());
            htmlFiles.put(path, content);
        }
        return new ConfigMapBuilder()
                .withNewMetadata()
                .withName(site.getMetadata().getName())
                .withNamespace(site.getMetadata().getNamespace())
                .endMetadata()
                .withData(htmlFiles)
                .build();
    }

    private String generateIndexHtml(List<PageCustomResource> pages) {
        StringBuilder sb = new StringBuilder("""
                    <html>
                      <head>
                        <style>
                          body {
                            font-family: sans-serif;
                            padding: 2rem;
                            background-color: #f9f9f9;
                          }
                          h1 {
                            color: #333;
                          }
                          ul {
                            list-style: none;
                            padding: 0;
                          }
                          li {
                            margin: 0.5rem 0;
                          }
                          a {
                            text-decoration: none;
                            color: #0066cc;
                          }
                          a:hover {
                            text-decoration: underline;
                          }
                        </style>
                      </head>
                      <body>
                        <h1>Index</h1>
                        <ul>
                """);

        for (PageCustomResource page : pages) {
            sb.append("<li><a href=\"")
                    .append(page.getSpec().path())
                    .append(".html\">")
                    .append(page.getMetadata().getName())
                    .append("</a></li>");
        }

        sb.append("""
                        </ul>
                      </body>
                    </html>
                """);

        return sb.toString();
    }

    private String wrapPageContent(String title, String bodyContent) {
        return """
                <html>
                  <head>
                    <style>
                      body {
                        font-family: sans-serif;
                        padding: 2rem;
                        background-color: #f9f9f9;
                      }
                      h1 {
                        color: #333;
                      }
                      a {
                        color: #0066cc;
                      }
                      a:hover {
                        text-decoration: underline;
                      }
                    </style>
                    <title>""" + title + """
                    </title>
                  </head>
                  <body>
                    <h1>""" + title + "</h1>" +
                    bodyContent + """
                  </body>
                </html>
                """;
    }

}
