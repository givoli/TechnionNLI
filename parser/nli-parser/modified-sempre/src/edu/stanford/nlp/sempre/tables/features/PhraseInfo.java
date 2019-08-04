//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre.tables.features;

import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.FuzzyMatchFn.FuzzyMatchFnMode;
import edu.stanford.nlp.sempre.tables.TableKnowledgeGraph;
import fig.basic.Option;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a phrase in the utterance.
 *
 * Also contains additional information such as POS and NER tags.
 *
 * @author ppasupat
 */
public class PhraseInfo {
  public static class Options {
    @Option(gloss = "Maximum number of tokens in a phrase")
    public int maxPhraseLength = 3;
    @Option(gloss = "Use lemma form only")
    public boolean usePhraseLemmaOnly = false;
    @Option(gloss = "Fuzzy match predicates")
    public boolean computeFuzzyMatchPredicates = false;
  }
  public static Options opts = new Options();

  public final int start, end, endOffset;
  public final String text;
  public final String lemmaText;
  public final List<String> tokens;
  public final List<String> lemmaTokens;
  public final List<String> posTags;
  public final List<String> nerTags;
  public final String canonicalPosSeq;
  public final List<String> fuzzyMatchedPredicates;

  public PhraseInfo(Example ex, int start, int end) {
    this.start = start;
    this.end = end;
    LanguageInfo languageInfo = ex.languageInfo;
    this.endOffset = end - languageInfo.numTokens();
    tokens = languageInfo.tokens.subList(start, end);
    lemmaTokens = languageInfo.tokens.subList(start, end);
    posTags = languageInfo.posTags.subList(start, end);
    nerTags = languageInfo.nerTags.subList(start, end);
    text = languageInfo.phrase(start, end).toLowerCase();
    lemmaText = languageInfo.lemmaPhrase(start, end).toLowerCase();
    canonicalPosSeq = languageInfo.canonicalPosSeq(start, end);
    fuzzyMatchedPredicates = opts.computeFuzzyMatchPredicates ? getFuzzyMatchedPredicates(ex.context) : null;
  }

  private List<String> getFuzzyMatchedPredicates(ContextValue context) {
    // modified this method. --Ofer Givoli
    List<Formula> formulas = new ArrayList<>();
    if (ExperimentRunner.isExperimentCurrentlyRunning()){
      return null;
    } else {
      if (context == null || context.graph == null || !(context.graph instanceof TableKnowledgeGraph))
        return null;
      TableKnowledgeGraph graph = (TableKnowledgeGraph) context.graph;
      // Assume everything is ValueFormula
      formulas.addAll(graph.getFuzzyMatchedFormulas(text, FuzzyMatchFnMode.ENTITY));
      formulas.addAll(graph.getFuzzyMatchedFormulas(text, FuzzyMatchFnMode.BINARY));
    }

    List<String> matchedPredicates = new ArrayList<>();
    for (Formula formula : formulas) {
      if (formula instanceof ValueFormula) {
        Value value = ((ValueFormula<?>) formula).value;
        if (value instanceof NameValue) {
          matchedPredicates.add(((NameValue) value).id);
        } else {
          throw new RuntimeException("Not a NameValue: " + value);
        }
      } else {
        throw new RuntimeException("Not a ValueFormula: " + formula);
      }
    }
    return matchedPredicates;
  }

  @Override
  public String toString() {
    return "\"" + text + "\"";
  }

  // Caching
  public static final Map<Example, List<PhraseInfo>> phraseInfosCache = new HashMap<>();

  public static synchronized List<PhraseInfo> getPhraseInfos(Example ex) {
    List<PhraseInfo> phraseInfos = phraseInfosCache.get(ex);
    if (phraseInfos == null) {
      phraseInfos = new ArrayList<>();
      List<String> tokens = ex.languageInfo.tokens;
      for (int s = 1; s <= opts.maxPhraseLength; s++) {
        for (int i = 0; i <= tokens.size() - s; i++) {
          phraseInfos.add(new PhraseInfo(ex, i, i + s));
        }
      }
      phraseInfosCache.put(ex, phraseInfos);
    }
    return phraseInfos;
  }

}
