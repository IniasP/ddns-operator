package eu.inias.ddnsoperator.model.cloudflare;

public record CloudflareApiResponse<T>(
        T result
) {
}
