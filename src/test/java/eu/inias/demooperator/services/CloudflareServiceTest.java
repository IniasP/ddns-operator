package eu.inias.demooperator.services;

import eu.inias.demooperator.model.cloudflare.CloudflareApiRecord;
import eu.inias.demooperator.model.cloudflare.CloudflareApiZone;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

@SpringBootTest
class CloudflareServiceTest {
    @Autowired
    CloudflareServiceFactory cloudflareServiceFactory;

    @Test
    void realApiTest() {
        CloudflareService cloudflareService = cloudflareServiceFactory.create("uulLpdTk978J16a2i0uV-cTQV_V-K1YlhmvpxHza");
        CloudflareApiZone zone = cloudflareService.getZoneByName("inias.eu");
//        String ip = restClient.get()
//                .uri("https://icanhazip.com")
//                .retrieve()
//                .body(String.class);
        Optional<CloudflareApiRecord> aRecord = cloudflareService.getDnsRecordByName(zone.id(), "ha.inias.eu");
        System.out.println();
    }
}