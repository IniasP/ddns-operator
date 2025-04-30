package eu.inias.demooperator.dependentresources;

import eu.inias.demooperator.crds.CloudflareRecordCustomResource;
import eu.inias.demooperator.crds.SiteCustomResource;
import io.fabric8.kubernetes.api.model.networking.v1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressRuleBuilder;
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
        String appName = "site-" + site.getMetadata().getName();
        return context.getSecondaryResource(CloudflareRecordCustomResource.class)
                .map(cloudflareRecord ->
                        new IngressBuilder()
                                .withNewMetadata()
                                .withName(appName)
                                .withNamespace(site.getMetadata().getNamespace())
                                .withLabels(Map.of("app", appName))
                                .endMetadata()
                                .withNewSpec()
                                .withIngressClassName("nginx")
                                .withRules(new IngressRuleBuilder()
                                        .withHost(cloudflareRecord.getStatus().host())
                                        .withNewHttp()
                                        .withPaths(new HTTPIngressPathBuilder()
                                                .withPath("/")
                                                .withPathType("Prefix")
                                                .withNewBackend()
                                                .withNewService()
                                                .withName(appName)
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
