package eu.inias.ddnsoperator.testconfig;

import eu.inias.ddnsoperator.crds.certmanager.CertificateCustomResource;
import eu.inias.ddnsoperator.stubs.TestCloudflareServiceFactory;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.config.ConfigurationServiceOverrider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Set;
import java.util.function.Consumer;

@TestConfiguration
public class OperatorTestConfiguration {
    @Bean
    public Consumer<ConfigurationServiceOverrider> disableSsaConfigurationServiceOverrider() {
        return overrider -> {
            overrider.withUseSSAToPatchPrimaryResource(false);
            overrider.withDefaultNonSSAResource(Set.of(ConfigMap.class, Secret.class, CertificateCustomResource.class));
        };
    }

    @Bean
    public TestCloudflareServiceFactory cloudflareServiceFactory() {
        return new TestCloudflareServiceFactory();
    }
}
