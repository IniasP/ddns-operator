package eu.inias.demooperator.dependentresources;

import eu.inias.demooperator.crds.SiteCustomResource;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.util.Map;

public class SiteServiceDependentResource
        extends CRUDKubernetesDependentResource<Service, SiteCustomResource> {
    public SiteServiceDependentResource() {
        super(Service.class);
    }

    @Override
    protected Service desired(SiteCustomResource site, Context<SiteCustomResource> context) {
        String siteName = site.getMetadata().getName();
        return new ServiceBuilder()
                .withNewMetadata()
                .withName(siteName)
                .withNamespace(site.getMetadata().getNamespace())
                .withLabels(Map.of("app", siteName))
                .endMetadata()
                .withNewSpec()
                .withSelector(Map.of("app", siteName))
                .withPorts(
                        new ServicePortBuilder()
                                .withProtocol("TCP")
                                .withPort(80)
                                .withTargetPort(new IntOrString(80))
                                .build()
                )
                .endSpec()
                .build();
    }
}
