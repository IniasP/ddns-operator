package eu.inias.ddnsoperator.reconcilers;

import eu.inias.ddnsoperator.crds.cloudflarerecord.CloudflareRecordCustomResource;
import eu.inias.ddnsoperator.crds.cloudflarerecord.CloudflareRecordStatus;
import eu.inias.ddnsoperator.crds.cloudflarezone.CloudflareZoneCustomResource;
import eu.inias.ddnsoperator.model.cloudflare.CloudflareApiRecord;
import eu.inias.ddnsoperator.services.CloudflareService;
import eu.inias.ddnsoperator.services.CloudflareServiceFactory;
import eu.inias.ddnsoperator.services.KubernetesService;
import eu.inias.ddnsoperator.services.PublicIpService;
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@ControllerConfiguration(
        // run frequently, because this also functions as a DDNS service
        maxReconciliationInterval = @MaxReconciliationInterval(interval = 5, timeUnit = TimeUnit.MINUTES)
)
public class CloudflareRecordReconciler
        implements Reconciler<CloudflareRecordCustomResource>, Cleaner<CloudflareRecordCustomResource> {
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
    public UpdateControl<CloudflareRecordCustomResource> reconcile(
            CloudflareRecordCustomResource recordResource,
            Context<CloudflareRecordCustomResource> context
    ) {
        String publicIp = publicIpService.getPublicIp();

        KubernetesService kubernetesService = new KubernetesService(context.getClient());

        CloudflareZoneCustomResource zoneResource = kubernetesService.getZone(recordResource);
        String zoneId = zoneResource.getStatus().id();
        if (zoneId == null) {
            LOGGER.warn(
                    "Zone id not available yet for Cloudflare zone {}, skipping reconciliation of record {}.",
                    zoneResource.getMetadata().getName(),
                    recordResource.getMetadata().getName()
            );
            return UpdateControl.noUpdate();
        }

        CloudflareService cloudflareService = getCloudflareService(kubernetesService, zoneResource);

        String zoneName = cloudflareService.getZoneById(zoneId).name();
        String host = recordResource.getSpec().name() + "." + zoneName;
        boolean proxied = recordResource.getSpec().proxied();
        CloudflareApiRecord cloudflareApiRecord = cloudflareService.getDnsRecordByName(zoneId, host)
                .map(existingRecord -> updateRecord(existingRecord, zoneId, publicIp, proxied, cloudflareService))
                .orElseGet(() -> createRecord(zoneId, host, publicIp, proxied, cloudflareService));

        CloudflareRecordStatus status = new CloudflareRecordStatus(
                recordResource.getMetadata().getGeneration(),
                cloudflareApiRecord.id(),
                publicIp,
                Instant.now(),
                host
        );
        recordResource.setStatus(status);
        LOGGER.info("Reconciled CloudflareRecord {}", recordResource.getMetadata().getName());
        return UpdateControl.patchStatus(recordResource);
    }

    @Override
    public DeleteControl cleanup(
            CloudflareRecordCustomResource recordResource,
            Context<CloudflareRecordCustomResource> context
    ) {
        KubernetesService kubernetesService = new KubernetesService(context.getClient());
        CloudflareZoneCustomResource zoneResource = kubernetesService.getZone(recordResource);
        CloudflareService cloudflareService = getCloudflareService(kubernetesService, zoneResource);
        String zoneId = zoneResource.getStatus().id();
        cloudflareService.getDnsRecordById(zoneId, recordResource.getStatus().id())
                .or(() -> {
                    String recordName = getCloudflareRecordName(recordResource, zoneResource);
                    return cloudflareService.getDnsRecordByName(zoneId, recordName);
                })
                .ifPresent(existingRecord -> cloudflareService.deleteDnsRecord(zoneId, existingRecord.id()));
        LOGGER.info("Deleted Cloudflare record {} ({})", recordResource.getMetadata().getName(), recordResource.getStatus().host());
        return DeleteControl.defaultDelete();
    }

    private static String getCloudflareRecordName(
            CloudflareRecordCustomResource recordResource,
            CloudflareZoneCustomResource zoneResource
    ) {
        return recordResource.getSpec().name() + "." + zoneResource.getSpec().domain();
    }

    @Override
    public List<EventSource<?, CloudflareRecordCustomResource>> prepareEventSources(
            EventSourceContext<CloudflareRecordCustomResource> context
    ) {
        // create an index to cache secondary -> primary
        String indexName = "cloudflare-record-zone";
        context.getPrimaryCache().addIndexer(indexName, p -> List.of(getIndexKey(getZoneResourceId(p))));

        // the obvious direction, using spec.zoneRef
        PrimaryToSecondaryMapper<CloudflareRecordCustomResource> primaryToSecondary = p -> Set.of(getZoneResourceId(p));

        // use index for the reverse direction
        SecondaryToPrimaryMapper<CloudflareZoneCustomResource> secondaryToPrimary = s -> context.getPrimaryCache()
                .byIndex(indexName, getIndexKey(ResourceID.fromResource(s)))
                .stream()
                .map(ResourceID::fromResource)
                .collect(Collectors.toSet());

        // build the config
        InformerEventSourceConfiguration<CloudflareZoneCustomResource> configuration =
                InformerEventSourceConfiguration.from(CloudflareZoneCustomResource.class, CloudflareRecordCustomResource.class)
                        .withPrimaryToSecondaryMapper(primaryToSecondary)
                        .withSecondaryToPrimaryMapper(secondaryToPrimary)
                        .build();
        return List.of(new InformerEventSource<>(configuration, context));
    }

    private CloudflareService getCloudflareService(
            KubernetesService kubernetesService,
            CloudflareZoneCustomResource zoneResource
    ) {
        String apiToken = kubernetesService.getCloudflareApiToken(zoneResource);
        return cloudflareServiceFactory.create(apiToken);
    }

    private static CloudflareApiRecord createRecord(
            String zoneId,
            String recordName,
            String publicIp,
            boolean proxied,
            CloudflareService cloudflareService
    ) {
        CloudflareApiRecord record = CloudflareApiRecord.newARecord(recordName, publicIp, proxied);
        return cloudflareService.createDnsRecord(zoneId, record);
    }

    private static CloudflareApiRecord updateRecord(
            CloudflareApiRecord existingRecord,
            String zoneId,
            String publicIp,
            boolean proxied,
            CloudflareService cloudflareService
    ) {
        if (existingRecord.content().equals(publicIp) && existingRecord.proxied() == proxied) {
            LOGGER.info("Record {} is up to date, nothing to do.", existingRecord.name());
            return existingRecord;
        } else {
            return cloudflareService.updateDnsRecord(zoneId, existingRecord.updated(publicIp, proxied));
        }
    }

    private ResourceID getZoneResourceId(CloudflareRecordCustomResource record) {
        return new ResourceID(record.getSpec().zoneRef(), record.getMetadata().getNamespace());
    }

    private static String getIndexKey(ResourceID id) {
        return id.getName() + "#" + id.getNamespace();
    }
}
