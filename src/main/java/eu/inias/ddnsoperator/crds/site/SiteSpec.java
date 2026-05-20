package eu.inias.ddnsoperator.crds.site;

import io.fabric8.generator.annotation.Required;
import io.fabric8.kubernetes.api.model.gatewayapi.v1.HTTPRouteRule;

import java.util.List;
import java.util.Map;

public record SiteSpec(
        @Required String cloudflareRecordRef,
        String indexTemplate,
        String pageTemplate,
        IngressSpec ingress,
        HttpRouteSpec httpRoute
) {
    public record IngressSpec(
            boolean enabled,
            String ingressClassName,
            Map<String, String> annotations
    ) {
        public IngressSpec {
            if (ingressClassName == null) {
                ingressClassName = "nginx";
            }
        }
    }

    public record HttpRouteSpec(
            boolean enabled,
            String gatewayName,
            String gatewayNamespace,
            String sectionName,
            CertIssuerRef certIssuerRef,
            List<HTTPRouteRule> rules
    ) {
        public HttpRouteSpec {
            if (gatewayName == null) {
                gatewayName = "nginx";
            }
        }
    }

    /**
     * Reference to a cert-manager Issuer or ClusterIssuer.
     * When present, the operator will create a cert-manager Certificate resource
     * in the gatewayNamespace so that TLS can be terminated at the Gateway.
     */
    public record CertIssuerRef(
            @Required String name,
            String kind,
            String group
    ) {
        public CertIssuerRef {
            if (kind == null) {
                kind = "ClusterIssuer";
            }
            if (group == null) {
                group = "cert-manager.io";
            }
        }
    }
}
