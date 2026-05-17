package eu.inias.ddnsoperator.crds.certmanager;

import java.util.List;

public record CertificateSpec(
        String secretName,
        List<String> dnsNames,
        IssuerRef issuerRef
) {
    public record IssuerRef(
            String name,
            String kind,
            String group
    ) {
    }
}
