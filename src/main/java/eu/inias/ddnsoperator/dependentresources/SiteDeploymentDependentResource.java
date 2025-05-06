package eu.inias.ddnsoperator.dependentresources;

import eu.inias.ddnsoperator.crds.site.SiteCustomResource;
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
        String siteName = site.getMetadata().getName();
        Map<String, String> labels = Map.of("app", siteName);
        return new DeploymentBuilder()
                .withNewMetadata()
                .withName(siteName)
                .withNamespace(site.getMetadata().getNamespace())
                .withLabels(labels)
                .withAnnotations(Map.of("reloader.stakater.com/auto", "true"))
                .endMetadata()
                .withNewSpec()
                .withReplicas(2)
                .withNewSelector()
                .withMatchLabels(labels)
                .endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withContainers(nginxContainer())
                .withVolumes(volume(siteName))
                .endSpec()
                .endTemplate()
                .endSpec()
                .build();
    }

    private static Container nginxContainer() {
        return new ContainerBuilder()
                .withName("nginx")
                .withImage("nginx:alpine")
                .withPorts(new ContainerPortBuilder().withContainerPort(80).build())
                .withVolumeMounts(volumeMount())
                .build();
    }

    private static VolumeMount volumeMount() {
        return new VolumeMountBuilder()
                .withName("site-content")
                .withMountPath("/usr/share/nginx/html")
                .build();
    }

    private static Volume volume(String siteName) {
        return new VolumeBuilder()
                .withName("site-content")
                .withNewConfigMap()
                .withName(siteName)
                .endConfigMap()
                .build();
    }
}
