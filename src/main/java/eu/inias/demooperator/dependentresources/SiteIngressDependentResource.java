package eu.inias.demooperator.dependentresources;

import eu.inias.demooperator.crds.cloudflarerecord.CloudflareRecordCustomResource;
import eu.inias.demooperator.crds.site.SiteCustomResource;
import io.fabric8.kubernetes.api.model.networking.v1.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.util.Map;

public class SiteIngressDependentResource
        extends CRUDKubernetesDependentResource<Ingress, SiteCustomResource> {
    public SiteIngressDependentResource() {
        super(Ingress.class);
    }

    @Override
    protected Ingress desired(SiteCustomResource site, Context<SiteCustomResource> context) {
        return context.getSecondaryResource(CloudflareRecordCustomResource.class)
                .map(cloudflareRecord -> ingress(site, cloudflareRecord))
                .orElse(null);
    }

    private static Ingress ingress(SiteCustomResource site, CloudflareRecordCustomResource cloudflareRecord) {
        String siteName = site.getMetadata().getName();
        return new IngressBuilder()
                .withNewMetadata()
                .withName(siteName)
                .withNamespace(site.getMetadata().getNamespace())
                .withLabels(Map.of("app", siteName))
                .withAnnotations(Map.of(
                        "cert-manager.io/cluster-issuer", "letsencrypt-prod",
                        "kubernetes.io/ingress.class", "nginx"
                ))
                .endMetadata()
                .withNewSpec()
                .withIngressClassName("nginx")
                .withTls(tls(cloudflareRecord.getStatus().host(), siteName + "-tls"))
                .withRules(ingressRule(cloudflareRecord, siteName))
                .endSpec()
                .build();
    }

    private static IngressRule ingressRule(CloudflareRecordCustomResource cloudflareRecord, String siteName) {
        return new IngressRuleBuilder()
                .withHost(cloudflareRecord.getStatus().host())
                .withNewHttp()
                .withPaths(new HTTPIngressPathBuilder()
                        .withPath("/")
                        .withPathType("Prefix")
                        .withNewBackend()
                        .withNewService()
                        .withName(siteName)
                        .withNewPort()
                        .withNumber(80)
                        .endPort()
                        .endService()
                        .endBackend()
                        .build())
                .endHttp()
                .build();
    }

    private static IngressTLS tls(String host, String secretName) {
        return new IngressTLSBuilder()
                .withHosts(host)
                .withSecretName(secretName)
                .build();
    }
}
