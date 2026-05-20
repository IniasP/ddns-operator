package eu.inias.ddnsoperator.crds.certmanager;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * Represents a cert-manager Certificate resource.
 * This is a minimal model sufficient for the operator to create/manage Certificates.
 * The CRD itself is owned by cert-manager, not by this operator.
 */
@Group("cert-manager.io")
@Version("v1")
@Kind("Certificate")
@Plural("certificates")
public class CertificateCustomResource
        extends CustomResource<CertificateSpec, Void>
        implements Namespaced {
}
