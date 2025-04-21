package eu.inias.demooperator.reconcilers;

import eu.inias.demooperator.crds.CloudflareRecord;
import eu.inias.demooperator.crds.CloudflareRecordStatus;
import eu.inias.demooperator.crds.CloudflareZone;
import eu.inias.demooperator.crds.SecretReference;
import eu.inias.demooperator.model.cloudflare.CloudflareApiRecord;
import eu.inias.demooperator.services.CloudflareService;
import eu.inias.demooperator.services.CloudflareServiceFactory;
import eu.inias.demooperator.services.PublicIpService;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@ControllerConfiguration(
        // run frequently, because this also functions as a DDNS service
        maxReconciliationInterval = @MaxReconciliationInterval(interval = 5, timeUnit = TimeUnit.MINUTES)
)
public class CloudflareRecordReconciler implements Reconciler<CloudflareRecord>, Cleaner<CloudflareRecord> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudflareRecordReconciler.class);

    private final PublicIpService publicIpService;
    private final CloudflareServiceFactory cloudflareServiceFactory;

    public CloudflareRecordReconciler(
            PublicIpService publicIpService,
            CloudflareServiceFactory cloudflareServiceFactory
    ) {
        this.publicIpService = publicIpService;
        this.cloudflareServiceFactory = cloudflareServiceFactory;
    }

    @Override
    public UpdateControl<CloudflareRecord> reconcile(
            CloudflareRecord recordResource,
            Context<CloudflareRecord> context
    ) {
        String publicIp = publicIpService.getPublicIp();

        CloudflareZone zoneResource = getZoneResource(recordResource, context.getClient());
        CloudflareService cloudflareService = getCloudflareService(zoneResource, context.getClient());
        String zoneName = zoneResource.getSpec().domain();
        String zoneId = cloudflareService.getZone(zoneName).id();
        String recordName = recordResource.getSpec().name() + "." + zoneName;
        cloudflareService.getDnsRecord(zoneId, recordName)
                .ifPresentOrElse(existingRecord -> {
                    if (existingRecord.content().equals(publicIp)) {
                        LOGGER.info("Ip is correctly set to {} for {}. Nothing to do.", publicIp, recordName);
                    } else {
                        cloudflareService.updateDnsRecord(zoneId, existingRecord.updated(publicIp));
                    }
                }, () -> {
                    cloudflareService.createDnsRecord(zoneId, CloudflareApiRecord.newARecord(recordName, publicIp));
                });

        recordResource.setStatus(getStatus(recordResource, publicIp));
        return UpdateControl.patchStatus(recordResource);
    }

    private CloudflareService getCloudflareService(CloudflareZone zoneResource, KubernetesClient client) {
        return cloudflareServiceFactory.create(getApiToken(zoneResource, client));
    }

    private static String getApiToken(CloudflareZone zoneResource, KubernetesClient client) {
        SecretReference apiTokenSecretRef = zoneResource.getSpec().apiTokenSecretRef();
        return getSecret(apiTokenSecretRef, zoneResource.getMetadata().getNamespace(), client);
    }

    private static String getSecret(SecretReference secretReference, String namespace, KubernetesClient client) {
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

    @Override
    public DeleteControl cleanup(CloudflareRecord recordResource, Context<CloudflareRecord> context) {
        CloudflareZone zoneResource = getZoneResource(recordResource, context.getClient());
        String zoneName = zoneResource.getSpec().domain();
        CloudflareService cloudflareService = getCloudflareService(zoneResource, context.getClient());
        String zoneId = cloudflareService.getZone(zoneName).id();
        cloudflareService.getDnsRecord(zoneId, recordResource.getSpec().name())
                .ifPresent(existingRecord -> cloudflareService.deleteDnsRecord(zoneId, existingRecord.id()));
        return DeleteControl.defaultDelete();
    }

    @Override
    public List<EventSource<?, CloudflareRecord>> prepareEventSources(EventSourceContext<CloudflareRecord> context) {
        String indexName = "cloudflare-record-zone";
        context.getPrimaryCache().addIndexer(indexName, p -> List.of(getIndexKey(getZoneResourceId(p))));
        PrimaryToSecondaryMapper<CloudflareRecord> primaryToSecondary = p -> Set.of(getZoneResourceId(p));
        SecondaryToPrimaryMapper<CloudflareZone> secondaryToPrimary = s -> context.getPrimaryCache()
                .byIndex(indexName, getIndexKey(ResourceID.fromResource(s)))
                .stream()
                .map(ResourceID::fromResource)
                .collect(Collectors.toSet());
        InformerEventSourceConfiguration<CloudflareZone> configuration =
                InformerEventSourceConfiguration.from(CloudflareZone.class, CloudflareRecord.class)
                        .withPrimaryToSecondaryMapper(primaryToSecondary)
                        .withSecondaryToPrimaryMapper(secondaryToPrimary)
                        .build();
        return List.of(new InformerEventSource<>(configuration, context));
    }

    private static CloudflareZone getZoneResource(CloudflareRecord recordResource, KubernetesClient client) {
        return client.resources(CloudflareZone.class)
                .withName(recordResource.getSpec().zoneRef())
                .require();
    }

    private static CloudflareRecordStatus getStatus(CloudflareRecord recordResource, String publicIp) {
        return new CloudflareRecordStatus(
                recordResource.getMetadata().getGeneration(),
                true,
                publicIp,
                Instant.now()
        );
    }

    private ResourceID getZoneResourceId(CloudflareRecord record) {
        return new ResourceID(record.getSpec().zoneRef(), record.getMetadata().getNamespace());
    }

    private static String getIndexKey(ResourceID id) {
        return id.getName() + "#" + id.getNamespace();
    }
}
