package eu.inias.demooperator.crds;

public record SecretReference(
        String name,
        String key
) {
}
