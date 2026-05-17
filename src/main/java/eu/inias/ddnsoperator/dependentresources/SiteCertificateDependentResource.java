package eu.inias.ddnsoperator.dependentresources;

import eu.inias.ddnsoperator.crds.certmanager.CertificateCustomResource;
import eu.inias.ddnsoperator.crds.certmanager.CertificateSpec;
import eu.inias.ddnsoperator.crds.cloudflarerecord.CloudflareRecordCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteSpec;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

import java.util.List;
import java.util.Map;

/**
 * Creates a cert-manager Certificate resource in the Gateway's namespace so that
 * TLS can be terminated at the Gateway. This mirrors what cert-manager's Ingress
 * annotation used to do automatically.
 *
 * The Certificate is only created when {@code httpRoute.certIssuerRef} is set.
 * The secret is created in {@code httpRoute.gatewayNamespace} (defaulting to the
 * Site's own namespace if not set), matching where the Gateway listener reads it from.
 */
public class SiteCertificateDependentResource
        extends CRUDKubernetesDependentResource<CertificateCustomResource, SiteCustomResource> {

    public SiteCertificateDependentResource() {
        super(CertificateCustomResource.class);
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
        String secretName = siteName + "-tls";

        // The Certificate must live in the same namespace as the Gateway,
        // because that is where the Gateway reads TLS secrets from.
        String certNamespace = httpRouteSpec.gatewayNamespace() != null
                ? httpRouteSpec.gatewayNamespace()
                : site.getMetadata().getNamespace();

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
