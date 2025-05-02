package eu.inias.demooperator.services;

import eu.inias.demooperator.crds.cloudflarerecord.CloudflareRecordCustomResource;
import eu.inias.demooperator.crds.cloudflarezone.CloudflareZoneCustomResource;
import eu.inias.demooperator.crds.SecretReference;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class KubernetesService {
    private final KubernetesClient client;

    public KubernetesService(KubernetesClient client) {
        this.client = client;
    }

    public String getCloudflareApiToken(CloudflareZoneCustomResource zoneResource) {
        SecretReference apiTokenSecretRef = zoneResource.getSpec().apiTokenSecretRef();
        return getSecret(apiTokenSecretRef, zoneResource.getMetadata().getNamespace());
    }

    private String getSecret(SecretReference secretReference, String namespace) {
        String secretBase64 = client.secrets()
                .inNamespace(namespace)
                .withName(secretReference.name())
                .require()
                .getData()
                .get(secretReference.key());
        if (secretBase64 == null) {
            throw new IllegalStateException(
                    "Key %s is not present in secret %s.".formatted(secretReference.key(), secretReference.name())
            );
        }
        return new String(Base64.getDecoder().decode(secretBase64), UTF_8);
    }

    public CloudflareZoneCustomResource getZone(CloudflareRecordCustomResource recordResource) {
        return client.resources(CloudflareZoneCustomResource.class)
                .inNamespace(recordResource.getMetadata().getNamespace())
                .withName(recordResource.getSpec().zoneRef())
                .require();
    }
}
