package eu.inias.demooperator.reconcilers;

import eu.inias.demooperator.crds.CloudflareZoneCustomResource;
import eu.inias.demooperator.crds.CloudflareZoneStatus;
import eu.inias.demooperator.crds.SecretReference;
import eu.inias.demooperator.exceptions.ReconciliationException;
import eu.inias.demooperator.model.cloudflare.CloudflareApiZone;
import eu.inias.demooperator.services.CloudflareServiceFactory;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.springframework.stereotype.Component;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@ControllerConfiguration
public class CloudflareZoneReconciler implements Reconciler<CloudflareZoneCustomResource> {
    private final CloudflareServiceFactory cloudflareServiceFactory;

    public CloudflareZoneReconciler(CloudflareServiceFactory cloudflareServiceFactory) {
        this.cloudflareServiceFactory = cloudflareServiceFactory;
    }

    @Override
    public UpdateControl<CloudflareZoneCustomResource> reconcile(
            CloudflareZoneCustomResource resource,
            Context<CloudflareZoneCustomResource> context
    ) {
        String cloudflareZoneName = resource.getSpec().domain();
        CloudflareApiZone zone = cloudflareServiceFactory.create(getApiToken(resource, context.getClient()))
                .getZoneByName(cloudflareZoneName)
                .orElseThrow(() -> new ReconciliationException("Zone %s not found".formatted(cloudflareZoneName)));
        resource.setStatus(new CloudflareZoneStatus(
                resource.getMetadata().getGeneration(),
                zone.id()
        ));
        return UpdateControl.patchStatus(resource);
    }

    // TODO: avoid duplication of these 2 methods (K8sResourcesService?)

    private static String getApiToken(CloudflareZoneCustomResource zoneResource, KubernetesClient client) {
        SecretReference apiTokenSecretRef = zoneResource.getSpec().apiTokenSecretRef();
        return getSecret(apiTokenSecretRef, zoneResource.getMetadata().getNamespace(), client);
    }

    private static String getSecret(
            SecretReference secretReference,
            String namespace,
            KubernetesClient client
    ) {
        String secretBase64 = client.secrets()
                .inNamespace(namespace)
                .withName(secretReference.name())
                .require()
                .getData()
                .get(secretReference.key());
        if (secretBase64 == null) {
            throw new IllegalStateException(
                    "Key %s is not present in secret %s.".formatted(secretReference.key(), secretReference.name())
            );
        }
        return new String(Base64.getDecoder().decode(secretBase64), UTF_8);
    }
}
