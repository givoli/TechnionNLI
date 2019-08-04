package il.ac.technion.nlp.nli.parser.features.phrase_predicate;

import org.jetbrains.annotations.Nullable;

/**
 * Defines the alignment between a string associated with a phrase (e.g. its lemma form) and some other string to be
 * compare with (referred to as "other").
 * The constants are declared in the order of the alignment importance (from strongest alignment to weakest).
 */
public enum UnlexicalizedAlignmentType {
    EQUALS("="),
    OTHER_IS_PREFIX("o*"),
    OTHER_IS_SUFFIX("*o"),
    PHRASE_STR_IS_PREFIX("p*"),
    PHRASE_STR_IS_SUFFIX("*p");

    public final String shortName;

    UnlexicalizedAlignmentType(String shortName) {
        this.shortName = shortName;
    }

    /**
     * @return null in case no unlexicalized feature should be extracted. Otherwise returns a comparison type that
     * fulfills ({@link #EQUALS} has precedence).
     */
    public static @Nullable
    UnlexicalizedAlignmentType create(String phraseStr, String other){
        if (other.equals(phraseStr))
            return EQUALS;
        if (other.startsWith(phraseStr))
            return PHRASE_STR_IS_PREFIX;
        if (other.endsWith(phraseStr))
            return PHRASE_STR_IS_SUFFIX;
        if (phraseStr.startsWith(other))
            return OTHER_IS_PREFIX;
        if (phraseStr.endsWith(other))
            return OTHER_IS_SUFFIX;
        return null;
    }

    public boolean weakerThan(UnlexicalizedAlignmentType other) {
        return ordinal() > other.ordinal();
    }
}
