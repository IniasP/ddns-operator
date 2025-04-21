package eu.inias.demooperator.model.cloudflare;

import java.util.List;

public record CloudflareApiResponse<T>(
        List<T> result
) {
}
