package eu.inias.demooperator.dependentresources;

import eu.inias.demooperator.crds.SiteCustomResource;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.util.Map;

public class SiteDeploymentDependentResource
        extends CRUDKubernetesDependentResource<Deployment, SiteCustomResource> {
    public SiteDeploymentDependentResource() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(SiteCustomResource site, Context<SiteCustomResource> context) {
        // TODO: extract this, configmap uses same name
        // TODO: it may also be possible to add a UUID here for uniqueness (calculate into status field)
        String appName = "site-" + site.getMetadata().getName();
        Map<String, String> labels = Map.of("app", appName);
        Container container = new ContainerBuilder()
                .withName("nginx")
                .withImage("nginx:alpine")
                .withPorts(new ContainerPortBuilder().withContainerPort(80).build())
                .withVolumeMounts(
                        new VolumeMountBuilder()
                                .withName("site-content")
                                .withMountPath("/usr/share/nginx/html")
                                .build()
                )
                .build();
        Volume volume = new VolumeBuilder()
                .withName("site-content")
                .withNewConfigMap()
                .withName(appName)
                .endConfigMap()
                .build();
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(appName)
                .withNamespace(site.getMetadata().getNamespace())
                .withLabels(labels)
                .withAnnotations(Map.of("reloader.stakater.com/auto", "true"))
                .endMetadata()
                .withNewSpec()
                .withReplicas(2)
                .withNewSelector()
                .addToMatchLabels("app", appName)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
                .withVolumes(volume)
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }
}
