package eu.inias.ddnsoperator;

import eu.inias.ddnsoperator.crds.SecretReference;
import eu.inias.ddnsoperator.crds.cloudflarerecord.CloudflareRecordCustomResource;
import eu.inias.ddnsoperator.crds.cloudflarerecord.CloudflareRecordSpec;
import eu.inias.ddnsoperator.crds.cloudflarezone.CloudflareZoneCustomResource;
import eu.inias.ddnsoperator.crds.cloudflarezone.CloudflareZoneSpec;
import eu.inias.ddnsoperator.crds.page.PageCustomResource;
import eu.inias.ddnsoperator.crds.page.PageSpec;
import eu.inias.ddnsoperator.crds.site.SiteCustomResource;
import eu.inias.ddnsoperator.crds.site.SiteSpec;
import eu.inias.ddnsoperator.model.cloudflare.CloudflareApiZone;
import eu.inias.ddnsoperator.services.PublicIpService;
import eu.inias.ddnsoperator.stubs.TestCloudflareService;
import eu.inias.ddnsoperator.stubs.TestCloudflareServiceFactory;
import eu.inias.ddnsoperator.testconfig.OperatorTestConfiguration;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.springboot.starter.test.EnableMockOperator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnableMockOperator(crdPaths = {
        "classpath:META-INF/fabric8/cloudflarerecords.ddns.inias.eu-v1.yml",
        "classpath:META-INF/fabric8/cloudflarezones.ddns.inias.eu-v1.yml",
        "classpath:META-INF/fabric8/pages.ddns.inias.eu-v1.yml",
        "classpath:META-INF/fabric8/sites.ddns.inias.eu-v1.yml",
})
@Import(OperatorTestConfiguration.class)
public class IntegrationTest {
    static final String NAMESPACE = "test";
    public static final CloudflareApiZone TEST_ZONE = new CloudflareApiZone(
            UUID.randomUUID().toString(),
            "example.com"
    );

    @Autowired
    KubernetesClient client;

    @Autowired
    TestCloudflareServiceFactory testCloudflareServiceFactory;

    @MockitoBean
    PublicIpService publicIpService;

    TestCloudflareService testCloudflareService;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("kubernetes.disable.autoConfig", "true");
    }

    @BeforeEach
    void setUp() {
        testCloudflareService = testCloudflareServiceFactory.cloudflareService;
        createTestNamespace();
        testCloudflareService.addZone(TEST_ZONE);
    }

    @Test
    void test() {
        when(publicIpService.getPublicIp()).thenReturn("1.1.1.1");

        createApiTokenSecret();
        createZone();
        createRecord();

        await().untilAsserted(() ->
                assertThat(testCloudflareService.getDnsRecordByName(TEST_ZONE.id(), "test.example.com"))
                        .isPresent()
                        .hasValueSatisfying(r -> assertThat(r.proxied()).isTrue())
        );

        createSite();
        createPage();

        await().untilAsserted(() -> {
            ConfigMap configMap = client.resources(ConfigMap.class)
                    .inNamespace(NAMESPACE)
                    .withName("test-site")
                    .get();
            assertThat(configMap).isNotNull();
            // ...
        });
    }

    private void createPage() {
        PageCustomResource pageResource = new PageCustomResource();
        pageResource.setMetadata(new ObjectMetaBuilder()
                .withNamespace(NAMESPACE)
                .withName("test-page")
                .build());
        pageResource.setSpec(new PageSpec(
                "test-site",
                "page-path",
                "Page Title",
                "content"
        ));
        client.resource(pageResource).create();
    }

    private void createSite() {
        SiteCustomResource siteResource = new SiteCustomResource();
        siteResource.setMetadata(new ObjectMetaBuilder()
                .withNamespace(NAMESPACE)
                .withName("test-site")
                .build());
        siteResource.setSpec(new SiteSpec("test-record", null, null));
        client.resource(siteResource).create();
    }

    private void createRecord() {
        CloudflareRecordCustomResource recordResource = new CloudflareRecordCustomResource();
        recordResource.setMetadata(new ObjectMetaBuilder()
                .withName("test-record")
                .withNamespace(NAMESPACE)
                .build());
        recordResource.setSpec(new CloudflareRecordSpec("test-zone", "test", true));
        client.resource(recordResource).create();
    }

    private void createZone() {
        CloudflareZoneCustomResource zoneResource = new CloudflareZoneCustomResource();
        zoneResource.setMetadata(new ObjectMetaBuilder()
                .withName("test-zone")
                .withNamespace(NAMESPACE)
                .build());
        zoneResource.setSpec(new CloudflareZoneSpec(
                "example.com",
                new SecretReference("api-token-secret", "api-token"))
        );
        client.resource(zoneResource).create();
    }

    private void createApiTokenSecret() {
        Secret apiTokenSecret = new SecretBuilder()
                .withNewMetadata()
                .withName("api-token-secret")
                .withNamespace(NAMESPACE)
                .endMetadata()
                .withData(Map.of("api-token", base64Encode("token!")))
                .build();
        client.resource(apiTokenSecret).create();
    }

    private String base64Encode(String data) {
        return new String(Base64.getEncoder().encode(data.getBytes(UTF_8)), UTF_8);
    }

    private void createTestNamespace() {
        client.namespaces().resource(new NamespaceBuilder()
                .withMetadata(new ObjectMetaBuilder().withName(NAMESPACE).build())
                .build());
    }
}
