package eu.inias.demooperator.crds;

public record SimpleStatus(
        Long observedGeneration,
        Boolean ready
) implements Status {
}
