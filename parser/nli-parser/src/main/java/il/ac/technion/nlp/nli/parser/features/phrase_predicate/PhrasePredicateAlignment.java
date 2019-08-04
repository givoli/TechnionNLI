package il.ac.technion.nlp.nli.parser.features.phrase_predicate;

import edu.stanford.nlp.sempre.tables.features.PredicateInfo;

public class PhrasePredicateAlignment {

    public final Phrase phrase;
    public final PredicateInfo predicate;
    public final UnlexicalizedAlignmentType alignmentType;

    public PhrasePredicateAlignment(Phrase phrase, PredicateInfo predicate, UnlexicalizedAlignmentType alignmentType) {
        this.phrase = phrase;
        this.predicate = predicate;
        this.alignmentType = alignmentType;
    }
}
