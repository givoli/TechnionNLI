//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre.tables.features;

import com.ofergivoli.ojavalib.data_structures.map.SafeLinkedHashMap;
import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.tables.features.PredicateInfo.PredicateType;
import fig.basic.LispTree;
import fig.basic.LogInfo;
import fig.basic.MapUtils;
import fig.basic.Option;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.features.PhraseAssociation;
import il.ac.technion.nlp.nli.parser.features.phrase_predicate.Phrase;
import il.ac.technion.nlp.nli.parser.features.phrase_predicate.PhrasePredicateAlignment;
import il.ac.technion.nlp.nli.parser.features.phrase_predicate.UnlexicalizedAlignmentType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// Modified this file. The original WikiTables functionality is not supported. --Ofer Givoli

/**
 * Extract features based on (phrase, predicate) pairs.
 *
 * - |phrase| is an n-gram from the utterance (usually n = 1)
 * - |predicate| is a predicate (LispTree leaf) from the formula
 *   Example: fb:cell_name.barack_obama, fb:row.row.name, argmax
 *
 * Properties of phrases: POS tags, length, word shapes, ...
 * Properties of predicates: category (entity / binary / keyword), ...
 * Properties of alignment: exact match, prefix match, suffix match, string contains, ...
 *
 * @author ppasupat
 */
public class PhrasePredicateFeatureComputer implements FeatureComputer {
  public static class Options {
    @Option(gloss = "Verbosity") public int verbose = 0;
    @Option(gloss = "Define features on partial derivations as well")
    public boolean defineOnPartialDerivs = true;
    @Option(gloss = "Also define features on prefix and suffix matches")
    public boolean usePrefixSuffixMatch = true;
    @Option(gloss = "Also define features with POS tags")
    public boolean usePosFeatures = true;
    @Option(gloss = "Define unlexicalized phrase-predicate features")
    public boolean unlexicalizedPhrasePredicate = true;
    @Option(gloss = "Define lexicalized phrase-predicate features")
    public boolean lexicalizedPhrasePredicate = true;
    @Option(gloss = "Maximum ngram length for lexicalize all pair features")
    public int maxNforLexicalizeAllPairs = -1; // changed default value. --Ofer Givoli
  }
  public static Options opts = new Options();

  // removed the field 'maxNforLexicalizeAllPairs', that was declared here. --Ofer Givoli

  public PhrasePredicateFeatureComputer() {
    // moved the initialization logic of the removed field 'maxNforLexicalizeAllPairs' from here to getMaxNforLexicalizeAllPairs(). --Ofer Givoli
  }

  @Override
  public void extractLocal(Example ex, Derivation deriv) {
    // Modified method. --Ofer Givoli
    if (!(FeatureExtractor.containsDomain("phrase-predicate")
          || FeatureExtractor.containsDomain("missing-predicate")
          || FeatureExtractor.containsDomain("phrase-formula"))) return;
    // Only compute features at the root, except when the partial option is set.
    if (!opts.defineOnPartialDerivs && !deriv.isRoot(ex.numTokens())) return;
    List<PhraseInfo> phraseInfos = PhraseInfo.getPhraseInfos(ex);
    if (ExperimentRunner.isExperimentCurrentlyRunning()) // added this if-block. --Ofer Givoli
      phraseInfos = getPhraseInfosWithoutMultipleElementsWithSameProcessedPhrase(
              ExperimentRunner.getCurrentExperiment().getCurrentInferenceData().phraseAssociation, phraseInfos);
    List<PredicateInfo> predicateInfos = PredicateInfo.getPredicateInfos(deriv); // removed argument. --Ofer Givoli
    if (opts.verbose >= 2) {
      LogInfo.logs("Example: %s", ex.utterance);
      LogInfo.logs("Phrases: %s", phraseInfos);
      LogInfo.logs("Derivation: %s", deriv);
      LogInfo.logs("Predicates: %s", predicateInfos);
    }
    if (FeatureExtractor.containsDomain("phrase-predicate")) {
      if (opts.defineOnPartialDerivs) {
        deriv.getTempState().put("p-p", new ArrayList<>(predicateInfos));
        // Subtract predicates from children
        Map<PredicateInfo, Integer> predicateInfoCounts = new HashMap<>();
        for (PredicateInfo predicateInfo : predicateInfos)
          MapUtils.incr(predicateInfoCounts, predicateInfo);
        if (deriv.children != null) {
          for (Derivation child : deriv.children) {
            @SuppressWarnings("unchecked")
            List<PredicateInfo> childPredicateInfos = (List<PredicateInfo>) child.getTempState().get("p-p");
            for (PredicateInfo predicateInfo : childPredicateInfos) {
              MapUtils.incr(predicateInfoCounts, predicateInfo, -1);
              if (predicateInfoCounts.get(predicateInfo)<0){
                // this is possible due to a rule with semantics involving a beta-reduce operation.
                predicateInfoCounts.remove(predicateInfo);
              }
            }
          }
        }
        extractPhrasePredicateFeatures(deriv, phraseInfos, predicateInfoCounts); // extracted this out. --Ofer Givoli
        if (!ExperimentRunner.isExperimentCurrentlyRunning()) // added this if block. --Ofer Givoli
        {
          throw new RuntimeException("not supported");
        }
      } else {
        throw new RuntimeException("not supported");
      }
    }
    if (FeatureExtractor.containsDomain("missing-predicate")) {
      extractMissing(ex, deriv, phraseInfos, predicateInfos);
    }
    if (FeatureExtractor.containsDomain("phrase-formula")) {
      extractPhraseFormula(ex, deriv, phraseInfos);
    }
  }

  /**
   * When multiple elements in 'phraseInfos' have the same processed phrase, only the first one appearing in that
   * list appears in the returned list.
   * @return The order of the returned list is defined by 'phraseInfos'.
   * Added. --Ofer Givoli
   */
  private List<PhraseInfo> getPhraseInfosWithoutMultipleElementsWithSameProcessedPhrase(
          PhraseAssociation phraseAssociation, List<PhraseInfo> phraseInfos) {

    SafeLinkedHashMap<String, PhraseInfo> processedPhraseToPhraseInfo = new SafeLinkedHashMap<>();
    phraseInfos.forEach(phraseInfo -> {
      String processedPhrase = phraseAssociation.processPhrase.apply(phraseInfo.text);
      if (!processedPhraseToPhraseInfo.safeContainsKey(processedPhrase))
        processedPhraseToPhraseInfo.putNewKey(processedPhrase, phraseInfo);
    });
    LinkedList<PhraseInfo> result = new LinkedList<>();
    processedPhraseToPhraseInfo.forEach((processedPhrase,phraseInfo)->result.add(phraseInfo));
    return result;
  }

  // extracted this out and modified. --Ofer Givoli
  private static void extractPhrasePredicateFeatures(Derivation deriv, List<PhraseInfo> phraseInfos,
                                                     Map<PredicateInfo, Integer> predicateInfoCounts) {
    for (PhraseInfo phraseInfo : phraseInfos) {
      for (Map.Entry<PredicateInfo, Integer> entry : predicateInfoCounts.entrySet()) {
        if (entry.getValue() > 0)
          extractFeaturesOfPair(deriv, phraseInfo, entry.getKey(), entry.getValue());
      }
    }
  }

  // Added. --Ofer Givoli
  public static void extractPhrasePredicateFeatures(Example ex, Derivation deriv, List<PredicateInfo> predicateInfos) {
    Map<PredicateInfo, Integer> predicateInfoCounts = new LinkedHashMap<>();
    predicateInfos.forEach(predicateInfo ->
            MapUtils.incr(predicateInfoCounts, predicateInfo));
    extractPhrasePredicateFeatures(deriv, PhraseInfo.getPhraseInfos(ex), predicateInfoCounts);
  }

  // ============================================================
  // Matching
  // ============================================================



  // Added. --Ofer Givoli
  private static void extractFeaturesOfPair(Derivation deriv, PhraseInfo phraseInfo,
                                            PredicateInfo predicateInfo, double factor) {

    PhraseAssociation phraseAssociation = ExperimentRunner.getCurrentExperiment().getCurrentInferenceData()
            .phraseAssociation;

    String processedPhrase = phraseAssociation.processPhrase.apply(phraseInfo.text);
    if (!processedPhrase.isEmpty()) {
      phraseAssociation.getProcessedPhrasesAssociatedWithPredicateInfo(predicateInfo)
              .forEach(processedPhraseAssociatedWithPredicateInfo ->
                      extractUnlexicalizedFeaturesOfPair(deriv, phraseInfo, processedPhrase, predicateInfo,
                              processedPhraseAssociatedWithPredicateInfo, factor));
    }

    if (ExperimentRunner.getCurrentExperiment().settings.usePredicatesThatAreProbablyDomainSpecificForExtractingLexicalizedPhrasePredicateFeatures ||
      !phraseAssociation.isPredicateProbablyDomainSpecific(predicateInfo)) {
      extractLexicalizedFeaturesOfPair(deriv, phraseInfo, predicateInfo, factor);
    }
  }

  // Extracted out and modified. --Ofer Givoli
  private static void extractLexicalizedFeaturesOfPair(
          Derivation deriv, PhraseInfo phraseInfo, PredicateInfo predicateInfo, double factor) {



    String phraseString = phraseInfo.lemmaText;
    String predicateString = predicateInfo.value == null ?
            "id:" + predicateInfo.idForNonValue :
            "value:" + predicateInfo.value.toString();

    if (!opts.lexicalizedPhrasePredicate || phraseInfo.end - phraseInfo.start > getMaxNforLexicalizeAllPairs()) {
      return;
    }

    deriv.addFeature("p-p", phraseString + ";" + predicateString, factor);

    if (predicateInfo.type != PredicateType.SUBFEATURE &&
            predicateInfo.type != PredicateType.KEYWORD)
      deriv.addFeature("p-p", phraseString + ";" + predicateInfo.type, factor);
  }

  //Added. --Ofer Givoli
  private static int getMaxNforLexicalizeAllPairs() {
    return Math.min(opts.maxNforLexicalizeAllPairs, PhraseInfo.opts.maxPhraseLength);
  }

  /**
   * Compares 'processedPhrase' and 'processedPhraseAssociatedWithPredicateInfo'.
   * Extracted out and modified. --Ofer Givoli
   */
  private static void extractUnlexicalizedFeaturesOfPair(
          Derivation deriv, PhraseInfo phraseInfo, String processedPhrase, PredicateInfo predicateInfo,
          String processedPhraseAssociatedWithPredicateInfo, double factor) {

    if (opts.unlexicalizedPhrasePredicate)
    {
      @Nullable UnlexicalizedAlignmentType alignmentType = null;
      if (processedPhrase.equals(processedPhraseAssociatedWithPredicateInfo)) {
        alignmentType = UnlexicalizedAlignmentType.EQUALS;
      } else if (opts.usePrefixSuffixMatch) {
        if (processedPhraseAssociatedWithPredicateInfo.startsWith(processedPhrase)) {
          alignmentType = UnlexicalizedAlignmentType.PHRASE_STR_IS_PREFIX;
        } else if (processedPhraseAssociatedWithPredicateInfo.endsWith(processedPhrase)) {
          alignmentType = UnlexicalizedAlignmentType.PHRASE_STR_IS_SUFFIX;
        } else if (processedPhrase.startsWith(processedPhraseAssociatedWithPredicateInfo)) {
          alignmentType = UnlexicalizedAlignmentType.OTHER_IS_PREFIX;
        } else if (processedPhrase.endsWith(processedPhraseAssociatedWithPredicateInfo)) {
          alignmentType = UnlexicalizedAlignmentType.OTHER_IS_SUFFIX;
        }
      }

      if (alignmentType != null) {
        deriv.localPhrasePredicateAlignments.add(
                new PhrasePredicateAlignment(
                        new Phrase(phraseInfo.start, phraseInfo.end), predicateInfo, alignmentType));
        addUnlexicalizedFeature(deriv, phraseInfo, predicateInfo, alignmentType.shortName, factor);
      }
    }
  }

  // Made this method static and modified it. --Ofer Givoli
  private static void addUnlexicalizedFeature(Derivation deriv, PhraseInfo phraseInfo, PredicateInfo predicateInfo,
                                              String featurePrefix, double factor) {

    if(!ExperimentRunner.isExperimentCurrentlyRunning() ||
            ExperimentRunner.getCurrentExperiment().settings.extractUnlexicalizedPhrasePredicateFeaturesWithoutPos)
      addUnlexicalizedFeature_auxiliary(deriv, predicateInfo, featurePrefix, factor);
    if (opts.usePosFeatures)
      addUnlexicalizedFeature_auxiliary(deriv, predicateInfo,
          featurePrefix + "," + phraseInfo.canonicalPosSeq, factor);
  }

  // Renamed, made this method static and modified it. --Ofer Givoli
  private static void addUnlexicalizedFeature_auxiliary(
          Derivation deriv, PredicateInfo predicateInfo, String featurePrefix, double factor) {

    deriv.addFeature("p-p(u)", featurePrefix, factor);
    deriv.addFeature("p-p(u)", featurePrefix + "," + predicateInfo.type, factor);
  }

  // ============================================================
  // Missing predicate features
  // ============================================================

  private void extractMissing(Example ex, Derivation deriv, List<PhraseInfo> phraseInfos, List<PredicateInfo> predicateInfos) {
    // modified this method. --Ofer Givoli
    // Only makes sense at the root
    if (!deriv.isRoot(ex.numTokens())) return;
    // Get the list of all relevant predicates
    Predicate<PredicateInfo> predicateInfoIsRelevant = predicateInfo2 ->
            predicateInfo2.type == PredicateType.RELATION || predicateInfo2.type == PredicateType.ENTITY ||
                    predicateInfo2.type == PredicateType.NLI_METHOD_NAME;

    SafeHashSet<PredicateInfo> relevantCanonicalPredicateInfosInLogicalForm =  predicateInfos.stream()
            .filter(predicateInfoIsRelevant)
            .map(PredicateInfo::getCanonicalForm)
            .collect(Collectors.toCollection(SafeHashSet::new));


    // See which predicates are missing!
    PhraseAssociation phraseAssociation = ExperimentRunner.getCurrentExperiment().getCurrentInferenceData()
            .phraseAssociation;
    for (PhraseInfo phraseInfo : phraseInfos) {
      phraseAssociation.getPredicateInfosAssociatedWithPhrase(phraseInfo.text).stream()
              .filter(predicateInfoIsRelevant)
              .filter(predicateInfo->!relevantCanonicalPredicateInfosInLogicalForm.safeContains(
                      predicateInfo.getCanonicalForm()))
              .forEach(predicateInfo->
                      deriv.addFeature("m-p", "type=" + predicateInfo.type));
    }
  }

  // ============================================================
  // Phrase - Formula features
  // ============================================================

  private void extractPhraseFormula(Example ex, Derivation deriv, List<PhraseInfo> phraseInfos) {
    if (deriv.formula instanceof LambdaFormula) return;
    // Get rough formula
    LispTree tree = PredicateInfo.normalizeFormula(ex, deriv);
    if (opts.verbose >= 2) {
      LogInfo.logs("original formula %s", deriv.formula);
      LogInfo.logs("normalized formula %s", tree);
    }
    deriv.addFeature("p-f", "" + tree);
    for (PhraseInfo phraseInfo : phraseInfos) {
      if (!PhraseInfo.opts.usePhraseLemmaOnly) {
        deriv.addFeature("p-f", phraseInfo.text + "(o);" + tree);
      }
      deriv.addFeature("p-f", phraseInfo.lemmaText + ";" + tree);
    }
  }

}
