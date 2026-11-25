package eu.inias.ddnsoperator.dependentresources;

import eu.inias.ddnsoperator.crds.site.SiteCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteSpec;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Reconcile preconditions for the optional Site dependents. JOSDK throws "Desired cannot be null"
 * when a dependent's {@code desired()} returns null, so optional resources must be skipped
 * through a precondition instead. When a precondition is not met JOSDK also deletes the
 * resource if it exists, which is what we want after a feature is switched off in the spec.
 */
public final class SiteConditions {
    private SiteConditions() {
    }

    public static class IngressEnabled implements Condition<HasMetadata, SiteCustomResource> {
        @Override
        public boolean isMet(DependentResource<HasMetadata, SiteCustomResource> dependentResource,
                             SiteCustomResource site, Context<SiteCustomResource> context) {
            SiteSpec.IngressSpec ingress = site.getSpec().ingress();
            return ingress != null && ingress.enabled();
        }
    }

    public static class HttpRouteEnabled implements Condition<HasMetadata, SiteCustomResource> {
        @Override
        public boolean isMet(DependentResource<HasMetadata, SiteCustomResource> dependentResource,
                             SiteCustomResource site, Context<SiteCustomResource> context) {
            SiteSpec.HttpRouteSpec httpRoute = site.getSpec().httpRoute();
            return httpRoute != null && httpRoute.enabled();
        }
    }

    public static class CertificateRequested implements Condition<HasMetadata, SiteCustomResource> {
        @Override
        public boolean isMet(DependentResource<HasMetadata, SiteCustomResource> dependentResource,
                             SiteCustomResource site, Context<SiteCustomResource> context) {
            SiteSpec.HttpRouteSpec httpRoute = site.getSpec().httpRoute();
            return httpRoute != null && httpRoute.enabled() && httpRoute.certIssuerRef() != null;
        }
    }
}
