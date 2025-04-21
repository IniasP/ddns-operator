package eu.inias.demooperator.model.cloudflare;

public record CloudflareApiRecord(
        String id,
        String name,
        String type,
        String content
) {
    public static CloudflareApiRecord newARecord(String name, String ip) {
        return new CloudflareApiRecord(null, name, "A", ip);
    }
    public CloudflareApiRecord updated(String newContent) {
        return new CloudflareApiRecord(id, name, "A", newContent);
    }
}
