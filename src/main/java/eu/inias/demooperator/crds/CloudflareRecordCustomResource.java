package eu.inias.demooperator.crds;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("demo.inias.eu")
@Version("v1")
@Kind("CloudflareRecord")
@Plural("cloudflarerecords")
public class CloudflareRecordCustomResource
        extends CustomResource<CloudflareRecordSpec, CloudflareRecordStatus>
        implements Namespaced {
}
