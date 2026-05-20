package eu.inias.ddnsoperator.crds.certmanager;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Minimal spec for a cert-manager Certificate resource.
 * Unknown fields (e.g. usages, renewBefore, duration) are intentionally ignored
 * so the operator can watch existing Certificates without failing to deserialize them.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CertificateSpec(
        String secretName,
        List<String> dnsNames,
        IssuerRef issuerRef
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IssuerRef(
            String name,
            String kind,
            String group
    ) {
    }
}
