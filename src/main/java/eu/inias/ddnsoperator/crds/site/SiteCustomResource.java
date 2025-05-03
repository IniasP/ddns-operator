package eu.inias.ddnsoperator.crds.site;

import eu.inias.ddnsoperator.crds.ObservedGenerationStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("ddns.inias.eu")
@Version("v1")
@Kind("Site")
@Plural("sites")
public class SiteCustomResource
        extends CustomResource<SiteSpec, ObservedGenerationStatus>
        implements Namespaced {
}
