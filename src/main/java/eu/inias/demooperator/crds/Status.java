package eu.inias.demooperator.crds;

public interface Status {
    Long observedGeneration();
    Boolean ready();
}
