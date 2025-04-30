package eu.inias.demooperator.model.cloudflare;

public record CloudflareApiResponse<T>(
        T result
) {
}
