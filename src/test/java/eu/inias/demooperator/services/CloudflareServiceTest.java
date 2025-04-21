package eu.inias.demooperator.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "cloudflare.api.bearer="
})
class CloudflareServiceTest {
    @Autowired
    CloudflareService cloudFlareService;

    @Test
    void realApiTest() {
        String toot = cloudFlareService.getZoneId();
        System.out.println();
    }
}