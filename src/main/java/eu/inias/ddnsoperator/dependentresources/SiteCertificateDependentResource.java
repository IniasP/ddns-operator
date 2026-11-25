package eu.inias.ddnsoperator.dependentresources;

import eu.inias.ddnsoperator.crds.certmanager.CertificateCustomResource;
import eu.inias.ddnsoperator.crds.certmanager.CertificateSpec;
import eu.inias.ddnsoperator.crds.cloudflarerecord.CloudflareRecordCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteSpec;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates a cert-manager Certificate resource in the Gateway's namespace so that
 * TLS can be terminated at the Gateway. This mirrors what cert-manager's Ingress
 * annotation used to do automatically.
 *
 * The Certificate is only created when {@code httpRoute.certIssuerRef} is set.
 * The secret is created in {@code httpRoute.gatewayNamespace} (defaulting to the
 * Site's own namespace if not set), matching where the Gateway listener reads it from.
 *
 * Deliberately not a {@code CRUDKubernetesDependentResource}: that is garbage-collected, so JOSDK
 * would add an owner reference to the Site, which Kubernetes forbids across namespaces.
 * Without it, JOSDK maps events back via annotations and deletes the Certificate on cleanup.
 */
public class SiteCertificateDependentResource
        extends KubernetesDependentResource<CertificateCustomResource, SiteCustomResource>
        implements Creator<CertificateCustomResource, SiteCustomResource>,
        Updater<CertificateCustomResource, SiteCustomResource>,
        Deleter<SiteCustomResource> {

    public SiteCertificateDependentResource() {
        super(CertificateCustomResource.class);
    }

    /**
     * Look the Certificate up by name instead of through the annotation index. The index only knows
     * Certificates that carry this operator's primary-name annotations, so an existing Certificate
     * without them (e.g. created before the annotations, or by hand) would look absent: JOSDK would
     * try to create it again and the API server would answer 409 Conflict on every reconcile.
     */
    @Override
    protected InformerEventSourceConfiguration.Builder<CertificateCustomResource> informerConfigurationBuilder(
            EventSourceContext<SiteCustomResource> context) {
        return super.informerConfigurationBuilder(context)
                .withPrimaryToSecondaryMapper((SiteCustomResource site) ->
                        Set.of(new ResourceID(secretName(site), certificateNamespace(site))));
    }

    private static String secretName(SiteCustomResource site) {
        return site.getMetadata().getName() + "-tls";
    }

    private static String certificateNamespace(SiteCustomResource site) {
        SiteSpec.HttpRouteSpec httpRoute = site.getSpec().httpRoute();
        return httpRoute != null && httpRoute.gatewayNamespace() != null
                ? httpRoute.gatewayNamespace()
                : site.getMetadata().getNamespace();
    }

    @Override
    protected CertificateCustomResource desired(SiteCustomResource site, Context<SiteCustomResource> context) {
        return context.getSecondaryResource(CloudflareRecordCustomResource.class)
                .map(cloudflareRecord -> certificate(site, cloudflareRecord))
                .orElse(null);
    }

    private CertificateCustomResource certificate(SiteCustomResource site, CloudflareRecordCustomResource cloudflareRecord) {
        SiteSpec.HttpRouteSpec httpRouteSpec = site.getSpec().httpRoute();
        if (httpRouteSpec == null || !httpRouteSpec.enabled() || httpRouteSpec.certIssuerRef() == null) {
            return null;
        }

        SiteSpec.CertIssuerRef certIssuerRef = httpRouteSpec.certIssuerRef();
        String siteName = site.getMetadata().getName();
        String hostname = cloudflareRecord.getStatus().host();
        String secretName = secretName(site);

        // The Certificate must live in the same namespace as the Gateway,
        // because that is where the Gateway reads TLS secrets from.
        String certNamespace = certificateNamespace(site);

        CertificateCustomResource certificate = new CertificateCustomResource();
        certificate.getMetadata().setName(secretName);
        certificate.getMetadata().setNamespace(certNamespace);
        certificate.getMetadata().setLabels(Map.of(
                "app", siteName,
                "managed-by", "ddns-operator"
        ));
        certificate.setSpec(new CertificateSpec(
                secretName,
                List.of(hostname),
                new CertificateSpec.IssuerRef(
                        certIssuerRef.name(),
                        certIssuerRef.kind(),
                        certIssuerRef.group()
                )
        ));
        return certificate;
    }
}
