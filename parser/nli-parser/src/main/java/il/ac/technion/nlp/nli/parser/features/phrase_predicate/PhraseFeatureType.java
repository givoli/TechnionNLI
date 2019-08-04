package il.ac.technion.nlp.nli.parser.features.phrase_predicate;

/**
 * Based on what we compare the phrase string with.
 */
public enum PhraseFeatureType {
    NLI_METHOD_NAME("method-name"),
    FIELD_RELATION_NAME("relation"),
    ENTITY_TYPE("entity-type"),
    ENTITY("entity");

    public final String shortName;

    PhraseFeatureType(String shortName) {
        this.shortName = shortName;
    }
}
