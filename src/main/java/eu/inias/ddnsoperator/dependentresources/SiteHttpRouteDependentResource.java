package eu.inias.ddnsoperator.dependentresources;

import eu.inias.ddnsoperator.crds.cloudflarerecord.CloudflareRecordCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteSpec;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRoute;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteBuilder;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.ParentReferenceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.util.List;
import java.util.Map;

public class SiteHttpRouteDependentResource
        extends CRUDKubernetesDependentResource<HTTPRoute, SiteCustomResource> {
    public SiteHttpRouteDependentResource() {
        super(HTTPRoute.class);
    }

    @Override
    protected HTTPRoute desired(SiteCustomResource site, Context<SiteCustomResource> context) {
        return context.getSecondaryResource(CloudflareRecordCustomResource.class)
                .map(cloudflareRecord -> httpRoute(site, cloudflareRecord))
                .orElse(null);
    }

    private HTTPRoute httpRoute(SiteCustomResource site, CloudflareRecordCustomResource cloudflareRecord) {
        SiteSpec.HttpRouteSpec httpRouteSpec = site.getSpec().httpRoute();
        if (httpRouteSpec == null || !httpRouteSpec.enabled()) {
            return null;
        }
        String gatewayName = httpRouteSpec.gatewayName();
        String gatewayNamespace = httpRouteSpec.gatewayNamespace();
        String sectionName = httpRouteSpec.sectionName();
        String siteName = site.getMetadata().getName();
        String hostname = cloudflareRecord.getStatus().host();

        ParentReferenceBuilder parentRefBuilder = new ParentReferenceBuilder()
                .withGroup("gateway.networking.k8s.io")
                .withKind("Gateway")
                .withName(gatewayName);
        if (gatewayNamespace != null) {
            parentRefBuilder.withNamespace(gatewayNamespace);
        }
        if (sectionName != null) {
            parentRefBuilder.withSectionName(sectionName);
        }

        return new HTTPRouteBuilder()
                .withNewMetadata()
                .withName(siteName)
                .withNamespace(site.getMetadata().getNamespace())
                .withLabels(Map.of("app", siteName))
                .endMetadata()
                .withNewSpec()
                .withHostnames(hostname)
                .withRules(httpRouteSpec.rules())
                .withParentRefs(List.of(parentRefBuilder.build()))
                .endSpec()
                .build();
    }
}
