package eu.inias.ddnsoperator.stubs;

import eu.inias.ddnsoperator.services.CloudflareService;
import eu.inias.ddnsoperator.services.CloudflareServiceFactory;

public class TestCloudflareServiceFactory extends CloudflareServiceFactory {
    public final TestCloudflareService cloudflareService;

    public TestCloudflareServiceFactory() {
        super(null);
        this.cloudflareService = new TestCloudflareService();
    }

    @Override
    public CloudflareService create(String bearerToken) {
        return cloudflareService;
    }
}
