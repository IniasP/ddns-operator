package eu.inias.demooperator.dependentresources;

import eu.inias.demooperator.crds.CloudflareRecordCustomResource;
import eu.inias.demooperator.crds.SiteCustomResource;
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
        String siteName = site.getMetadata().getName();
        return context.getSecondaryResource(CloudflareRecordCustomResource.class)
                .map(cloudflareRecord ->
                        new IngressBuilder()
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
                                .withTls(new IngressTLSBuilder()
                                        .withHosts(cloudflareRecord.getStatus().host())
                                        .withSecretName(siteName + "-tls")
                                        .build())
                                .withRules(new IngressRuleBuilder()
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
                                        .build())
                                .endSpec()
                                .build())
                .orElse(null);
    }
}
