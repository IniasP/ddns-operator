package eu.inias.ddnsoperator.crds.cloudflarerecord;

import io.fabric8.generator.annotation.Default;
import io.fabric8.generator.annotation.Required;

public record CloudflareRecordSpec(
        @Required String zoneRef,
        @Required String name,
        @Default("true") boolean proxied
) {
}
