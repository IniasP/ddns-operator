package eu.inias.ddnsoperator.crds.page;

import eu.inias.ddnsoperator.crds.ObservedGenerationStatus;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("ddns.inias.eu")
@Version("v1")
@Kind("Page")
@Plural("pages")
public class PageCustomResource
        extends CustomResource<PageSpec, ObservedGenerationStatus>
        implements Namespaced {
}
