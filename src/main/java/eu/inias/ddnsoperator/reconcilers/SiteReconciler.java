package eu.inias.ddnsoperator.reconcilers;

import eu.inias.ddnsoperator.crds.cloudflarerecord.CloudflareRecordCustomResource;
import eu.inias.ddnsoperator.crds.ObservedGenerationStatus;
import eu.inias.ddnsoperator.crds.page.PageCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteCustomResource;
import eu.inias.ddnsoperator.dependentresources.SiteConfigMapDependentResource;
import eu.inias.ddnsoperator.dependentresources.SiteDeploymentDependentResource;
import eu.inias.ddnsoperator.dependentresources.SiteIngressDependentResource;
import eu.inias.ddnsoperator.dependentresources.SiteServiceDependentResource;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@ControllerConfiguration
@Workflow(dependents = {
        @Dependent(
                name = "site-configmap",
                type = SiteConfigMapDependentResource.class
        ),
        @Dependent(
                name = "site-deployment",
                type = SiteDeploymentDependentResource.class,
                dependsOn = "site-configmap"
        ),
        @Dependent(
                name = "site-service",
                type = SiteServiceDependentResource.class,
                dependsOn = "site-deployment"
        ),
        @Dependent(
                name = "site-ingress",
                type = SiteIngressDependentResource.class,
                dependsOn = "site-service"
        )
})
public class SiteReconciler implements Reconciler<SiteCustomResource>, Cleaner<SiteCustomResource> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SiteReconciler.class);

    @Override
    public UpdateControl<SiteCustomResource> reconcile(SiteCustomResource siteResource, Context<SiteCustomResource> context) {
        siteResource.setStatus(new ObservedGenerationStatus(siteResource.getMetadata().getGeneration()));
        LOGGER.info("Reconciled CloudflareRecord {}", siteResource.getMetadata().getName());
        return UpdateControl.patchStatus(siteResource);
    }

    @Override
    public List<EventSource<?, SiteCustomResource>> prepareEventSources(EventSourceContext<SiteCustomResource> context) {
        return List.of(getCloudflareRecordEventSource(context), getPageEventSource(context));
    }

    private static InformerEventSource<PageCustomResource, SiteCustomResource>
    getPageEventSource(EventSourceContext<SiteCustomResource> context) {
        SecondaryToPrimaryMapper<PageCustomResource> secondaryToPrimary = page -> Set.of(
                new ResourceID(page.getSpec().siteRef(), page.getMetadata().getNamespace())
        );
        InformerEventSourceConfiguration<PageCustomResource> configuration =
                InformerEventSourceConfiguration.from(PageCustomResource.class, SiteCustomResource.class)
                        .withSecondaryToPrimaryMapper(secondaryToPrimary)
                        .build();
        return new InformerEventSource<>(configuration, context);
    }

    private static InformerEventSource<CloudflareRecordCustomResource, SiteCustomResource>
    getCloudflareRecordEventSource(EventSourceContext<SiteCustomResource> context) {
        String indexName = "site-cloudflare-record";
        context.getPrimaryCache().addIndexer(indexName, site -> List.of(getIndexKey(getCloudflareRecordResourceId(site))));
        PrimaryToSecondaryMapper<SiteCustomResource> primaryToSecondary = site -> Set.of(getCloudflareRecordResourceId(site));
        SecondaryToPrimaryMapper<CloudflareRecordCustomResource> secondaryToPrimary = record -> context.getPrimaryCache()
                .byIndex(indexName, getIndexKey(ResourceID.fromResource(record)))
                .stream()
                .map(ResourceID::fromResource)
                .collect(Collectors.toSet());
        InformerEventSourceConfiguration<CloudflareRecordCustomResource> configuration =
                InformerEventSourceConfiguration.from(CloudflareRecordCustomResource.class, SiteCustomResource.class)
                        .withPrimaryToSecondaryMapper(primaryToSecondary)
                        .withSecondaryToPrimaryMapper(secondaryToPrimary)
                        .build();
        return new InformerEventSource<>(configuration, context);
    }

    private static ResourceID getCloudflareRecordResourceId(SiteCustomResource siteResource) {
        return new ResourceID(siteResource.getSpec().cloudflareRecordRef(), siteResource.getMetadata().getNamespace());
    }

    private static String getIndexKey(ResourceID id) {
        return id.getName() + "#" + id.getNamespace();
    }

    @Override
    public DeleteControl cleanup(SiteCustomResource siteCustomResource, Context<SiteCustomResource> context) {
        return DeleteControl.defaultDelete();
    }
}
