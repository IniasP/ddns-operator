package eu.inias.ddnsoperator.crds;

public record SecretReference(
        String name,
        String key
) {
}
