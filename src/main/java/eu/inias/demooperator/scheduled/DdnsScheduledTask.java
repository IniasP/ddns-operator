package eu.inias.demooperator.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This only works for single-node clusters.
 */
@Component
public class DdnsScheduledTask {
    @Scheduled(cron="0 */5 * * * *")
    public void updateDdns() {

    }
}
