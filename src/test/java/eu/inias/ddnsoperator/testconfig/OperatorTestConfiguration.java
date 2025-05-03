package eu.inias.ddnsoperator.testconfig;

import eu.inias.ddnsoperator.stubs.TestCloudflareServiceFactory;
import io.javaoperatorsdk.operator.api.config.ConfigurationServiceOverrider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.function.Consumer;

@TestConfiguration
public class OperatorTestConfiguration {
    @Bean
    public Consumer<ConfigurationServiceOverrider> disableSsaConfigurationServiceOverrider() {
        return overrider -> {
            overrider.withUseSSAToPatchPrimaryResource(false);
        };
    }

    @Bean
    public TestCloudflareServiceFactory cloudflareServiceFactory() {
        return new TestCloudflareServiceFactory();
    }
}
