package il.ac.technion.nlp.nli.parser.features.phrase_predicate;

import java.util.Objects;

public class Phrase {

    /**
     * Corresponds to {@link edu.stanford.nlp.sempre.tables.features.PhraseInfo#start}.
     */
    public final int start;

    /**
     * Corresponds to {@link edu.stanford.nlp.sempre.tables.features.PhraseInfo#end}.
     */
    public final int end;

    public Phrase(int start, int end) {
        this.start = start;
        this.end = end;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phrase phrase = (Phrase) o;
        return start == phrase.start &&
                end == phrase.end;
    }

    @Override
    public int hashCode() {

        return Objects.hash(start, end);
    }
}
