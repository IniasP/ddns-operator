package eu.inias.ddnsoperator.model.cloudflare;

public record CloudflareApiRecord(
        String id,
        String name,
        String type,
        String content,
        boolean proxied
) {
    public static CloudflareApiRecord newARecord(String name, String ip, boolean proxied) {
        return new CloudflareApiRecord(null, name, "A", ip, proxied);
    }

    public CloudflareApiRecord updated(String newContent) {
        return new CloudflareApiRecord(id, name, "A", newContent, proxied);
    }
}
