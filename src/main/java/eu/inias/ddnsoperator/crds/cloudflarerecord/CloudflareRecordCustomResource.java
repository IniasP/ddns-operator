package eu.inias.ddnsoperator.crds.cloudflarerecord;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("ddns.inias.eu")
@Version("v1")
@Kind("CloudflareRecord")
@Plural("cloudflarerecords")
public class CloudflareRecordCustomResource
        extends CustomResource<CloudflareRecordSpec, CloudflareRecordStatus>
        implements Namespaced {
}
