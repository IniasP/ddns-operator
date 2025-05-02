package eu.inias.demooperator.reconcilers;

import eu.inias.demooperator.crds.cloudflarezone.CloudflareZoneCustomResource;
import eu.inias.demooperator.crds.cloudflarezone.CloudflareZoneStatus;
import eu.inias.demooperator.exceptions.ReconciliationException;
import eu.inias.demooperator.model.cloudflare.CloudflareApiZone;
import eu.inias.demooperator.services.CloudflareServiceFactory;
import eu.inias.demooperator.services.KubernetesService;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@ControllerConfiguration
public class CloudflareZoneReconciler implements Reconciler<CloudflareZoneCustomResource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudflareZoneReconciler.class);

    private final CloudflareServiceFactory cloudflareServiceFactory;

    public CloudflareZoneReconciler(CloudflareServiceFactory cloudflareServiceFactory) {
        this.cloudflareServiceFactory = cloudflareServiceFactory;
    }

    @Override
    public UpdateControl<CloudflareZoneCustomResource> reconcile(
            CloudflareZoneCustomResource zoneResource,
            Context<CloudflareZoneCustomResource> context
    ) {
        String cloudflareZoneName = zoneResource.getSpec().domain();
        KubernetesService kubernetesService = new KubernetesService(context.getClient());
        String apiToken = kubernetesService.getCloudflareApiToken(zoneResource);
        CloudflareApiZone zone = cloudflareServiceFactory.create(apiToken)
                .getZoneByName(cloudflareZoneName)
                .orElseThrow(() -> new ReconciliationException("Zone %s not found".formatted(cloudflareZoneName)));
        zoneResource.setStatus(new CloudflareZoneStatus(
                zoneResource.getMetadata().getGeneration(),
                zone.id()
        ));
        LOGGER.info("Reconciled CloudflareZone {}", zoneResource.getMetadata().getName());
        return UpdateControl.patchStatus(zoneResource);
    }
}
