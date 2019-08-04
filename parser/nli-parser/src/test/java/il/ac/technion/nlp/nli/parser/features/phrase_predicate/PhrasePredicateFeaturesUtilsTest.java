package il.ac.technion.nlp.nli.parser.features.phrase_predicate;

import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.tables.features.PredicateInfo;
import il.ac.technion.nlp.nli.parser.features.GeneralFeatureTestUtils;
import org.junit.Before;

public class PhrasePredicateFeaturesUtilsTest {

    private Derivation rootDeriv = GeneralFeatureTestUtils.createDerivation();
    private Derivation childDeriv = GeneralFeatureTestUtils.createDerivation();
    private Derivation childOfChildDeriv = GeneralFeatureTestUtils.createDerivation();

    Phrase phrase1 = new Phrase(3,3);
    Phrase phrase2 = new Phrase(3,4);

    PredicateInfo predicate1 = new PredicateInfo("p1", PredicateInfo.PredicateType.ENTITY);
    PredicateInfo predicate2 = new PredicateInfo("p2", PredicateInfo.PredicateType.RELATION);


    @Before
    public void setUp() {
        rootDeriv.children.add(childDeriv);
        childDeriv.children.add(childOfChildDeriv);
    }

//TODO: clean this up if not used (since no longer using the multi phrase-predicate alignment features).
//    @Test
//    public void testNoAlignmentsAndSingleAlignment() {
//
//        // no alignments
//        extractLocalFeaturesFromAllDerivations();
//        verifyNoLocalFeaturesInDerivation();
//
//        // a single alignment
//        addPhrasePredicateAlignment(rootDeriv, phrase1, predicate1);
//        PhrasePredicateFeaturesUtils.extractLocalFeaturesAboutMultipleMatchesPerPhraseInfo(rootDeriv);
//        verifyNoLocalFeaturesInDerivation();
//    }
//
//
//    @Test
//    public void testTwoDifferentPhrasesAlignedWithSamePredicateInTheSameDerivation() {
//        addPhrasePredicateAlignment(childDeriv, phrase1, predicate1);
//        addPhrasePredicateAlignment(childDeriv, phrase2, predicate1);
//        extractLocalFeaturesFromAllDerivations();
//        verifyNoLocalFeaturesInDerivation();
//    }
//
//    @Test
//    public void testTwoDifferentPhrasesAlignedWithDifferentPredicatesInDifferentDerivations() {
//        addPhrasePredicateAlignment(childDeriv, phrase1, predicate1);
//        addPhrasePredicateAlignment(rootDeriv, phrase2, predicate2);
//        extractLocalFeaturesFromAllDerivations();
//        verifyNoLocalFeaturesInDerivation();
//    }
//
//
//    @SuppressWarnings("Duplicates")
//    @Test
//    public void testOnePhraseAlignedWithTwoPredicatesInTheSameDerivation() {
//        addPhrasePredicateAlignment(childDeriv, phrase1, predicate1);
//        addPhrasePredicateAlignment(childDeriv, phrase1, predicate2);
//        extractLocalFeaturesFromAllDerivations();
//        verifyNoLocalFeatures(childOfChildDeriv);
//        verifyLocalFeaturesExist(childDeriv);
//        verifyNoLocalFeatures(rootDeriv);
//    }
//
//    @SuppressWarnings("Duplicates")
//    @Test
//    public void testOnePhraseAlignedWithOnePredicateInDifferentDerivations() {
//        addPhrasePredicateAlignment(childOfChildDeriv, phrase1, predicate1);
//        addPhrasePredicateAlignment(childDeriv, phrase1, predicate1);
//        extractLocalFeaturesFromAllDerivations();
//        verifyNoLocalFeatures(childOfChildDeriv);
//        verifyLocalFeaturesExist(childDeriv);
//        verifyNoLocalFeatures(rootDeriv);
//    }
//
//
//    private void extractLocalFeaturesFromAllDerivations() {
//        PhrasePredicateFeaturesUtils.extractLocalFeaturesAboutMultipleMatchesPerPhraseInfo(childOfChildDeriv);
//        PhrasePredicateFeaturesUtils.extractLocalFeaturesAboutMultipleMatchesPerPhraseInfo(childDeriv);
//        PhrasePredicateFeaturesUtils.extractLocalFeaturesAboutMultipleMatchesPerPhraseInfo(rootDeriv);
//    }
//
//    private void verifyNoLocalFeaturesInDerivation() {
//        verifyNoLocalFeatures(childOfChildDeriv);
//        verifyNoLocalFeatures(childDeriv);
//        verifyNoLocalFeatures(rootDeriv);
//    }
//
//
//
//    private static void verifyLocalFeaturesExist(Derivation deriv) {
//        assertFalse(deriv.getLocalFeatureVector().toMap().isEmpty());
//    }
//
//    private static void verifyNoLocalFeatures(Derivation deriv) {
//        assertTrue(deriv.getLocalFeatureVector().toMap().isEmpty());
//    }
//
//    private void addPhrasePredicateAlignment(Derivation deriv, Phrase phrase, PredicateInfo predicate) {
//        deriv.localPhrasePredicateAlignments.add(new PhrasePredicateAlignment(phrase, predicate,
//                UnlexicalizedAlignmentType.EQUALS ));
//    }

}