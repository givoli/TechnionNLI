//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre.tables.features;

import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.tables.TableTypeSystem;
import fig.basic.LispTree;
import fig.basic.LogInfo;
import fig.basic.Option;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Extract features based on (phrase, denotation) pairs.
 * Intuition: "when" usually matches a date, which "how many" usually matches a number.
 *
 * @author ppasupat
 */
public class PhraseDenotationFeatureComputer implements FeatureComputer {
  public static class Options {
    @Option(gloss = "Verbosity")
    public int verbose = 0;
    @Option(gloss = "Look for the type under the first cell property")
    public boolean lookUnderCellProperty = false;
    @Option(gloss = "Define features for generic cell types too")
    public boolean useGenericCellType = false;
  }
  public static Options opts = new Options();

  @Override
  public void extractLocal(Example ex, Derivation deriv) {
    if (!(FeatureExtractor.containsDomain("custom-denotation")
          || FeatureExtractor.containsDomain("phrase-denotation")
          || FeatureExtractor.containsDomain("headword-denotation"))) return;
    // Only compute features at the root.
    if (!deriv.isRoot(ex.numTokens())) return;
    Collection<String> denotationTypes = tableTypes(deriv);
    extractCustomDenotationFeatures(ex, deriv, denotationTypes);
    extractPhraseDenotationFeatures(ex, deriv, denotationTypes);
    extractHeadwordDenotationFeatures(ex, deriv, denotationTypes);
  }

  public static Collection<String> tableTypes(Derivation deriv) {
    Set<String> denotationTypes = new HashSet<>();
    // Type based on SemType
    populateSemType("", deriv.type, denotationTypes);
    // Look for the type under the first cell property
    if (opts.lookUnderCellProperty) {
      Formula formula = deriv.formula;
      if (formula instanceof JoinFormula) {
        JoinFormula join = (JoinFormula) formula;
        String property = getCellProperty(join.relation);
        if (property != null) {
          populateSemType(property + "/", TypeInference.inferType(join.child), denotationTypes);
        }
      }
    }
    if (denotationTypes.isEmpty()) denotationTypes.add("OTHER");
    return denotationTypes;
  }

  private static void populateSemType(String prefix, SemType type, Collection<String> denotationTypes) {
    LispTree tree = type.toLispTree();
    if (tree.isLeaf()) {
      denotationTypes.add(prefix + tree.value);
    } else {
      for (LispTree subtree : tree.children) {
        if (!subtree.isLeaf()) continue;
        if (subtree.value.startsWith(TableTypeSystem.CELL_SPECIFIC_TYPE_PREFIX)) {
          denotationTypes.add(prefix + subtree.value);
          if (opts.useGenericCellType)
            denotationTypes.add(prefix + TableTypeSystem.CELL_GENERIC_TYPE);
        }
      }
    }
  }

  private static String getCellProperty(Formula formula) {
    LispTree tree = formula.toLispTree();
    if (tree.isLeaf()) {
      String value = tree.value;
      if (value.charAt(0) == '!' && value.substring(1).startsWith(TableTypeSystem.CELL_PROPERTY_NAME_PREFIX))
        return value;
    } else {
      if ("reverse".equals(tree.child(0).value) && tree.child(1).value.startsWith(TableTypeSystem.CELL_PROPERTY_NAME_PREFIX))
        return "!" + tree.child(1).value;
    }
    return null;
  }

  // ============================================================
  // Custom Denotation Features
  // ============================================================

  private void extractCustomDenotationFeatures(Example ex, Derivation deriv, Collection<String> denotationTypes) {
    if (!FeatureExtractor.containsDomain("custom-denotation")) return;

    if (deriv.value instanceof ErrorValue) {
      deriv.addFeature("custom-denotation", "error");
      return;
    } else if (deriv.value instanceof ListValue) {
      ListValue list = (ListValue) deriv.value;
      int size = list.values.size();
      deriv.addFeature("custom-denotation", "size" + (size < 3 ? "=" + size : ">=" + 3));
      if (size == 1) {
        Value value = list.values.get(0);
        if (value instanceof NumberValue) {
          double number = ((NumberValue) value).value;
          deriv.addFeature("custom-denotation", "number" + (number > 0 ? ">0" : number == 0 ? "=0" : "<0"));
          deriv.addFeature("custom-denotation", "number" + ((int) number == number ? "-int" : "-frac"));
        }
      }
    }
  }

  // ============================================================
  // Phrase - Denotation
  // ============================================================

  private void extractPhraseDenotationFeatures(Example ex, Derivation deriv, Collection<String> denotationTypes) {
    // modified Method. --Ofer Givoli
    if (!FeatureExtractor.containsDomain("phrase-denotation")) return;
    List<PhraseInfo> phraseInfos = PhraseInfo.getPhraseInfos(ex);
    if (opts.verbose >= 2)
      LogInfo.logs("%s %s %s", deriv.value, deriv.type, denotationTypes);
    for (String denotationType : denotationTypes) {
      for (PhraseInfo phraseInfo : phraseInfos) {
        if (!PhraseInfo.opts.usePhraseLemmaOnly) {
          deriv.addFeature("p-d", "(o)" + phraseInfo.text + ";" + denotationType);
        }
        deriv.addFeature("p-d", phraseInfo.lemmaText + ";" + denotationType);
      }
      // Check original column text
      String[] tokens = denotationType.split("/");
      String actualType = tokens[tokens.length - 1], suffix = (tokens.length == 1) ? "" : "(" + tokens[0] + ")";
      String originalColumn;
      throw new RuntimeException("not supported."); // added (in place of code that was no longer compilable). --Ofer Givoli
    }
  }

  // ============================================================
  // Headword - Denotation
  // ============================================================

  private void extractHeadwordDenotationFeatures(Example ex, Derivation deriv, Collection<String> denotationTypes) {
    // modified Method. --Ofer Givoli
    if (!FeatureExtractor.containsDomain("headword-denotation")) return;
    HeadwordInfo headwordInfo = HeadwordInfo.analyze(ex.languageInfo);
    if (headwordInfo == null) return;
    if (opts.verbose >= 2)
      LogInfo.logs("%s [%s] | %s %s %s", ex.utterance, headwordInfo, deriv.value, deriv.type, denotationTypes);
    for (String denotationType : denotationTypes) {
      deriv.addFeature("h-d", headwordInfo + ";" + denotationType);
      deriv.addFeature("h-d", headwordInfo.questionWordTuple() + ";" + denotationType);
      deriv.addFeature("h-d", headwordInfo.headwordTuple() + ";" + denotationType);
      // Check original column text
      String[] tokens = denotationType.split("/");
      String actualType = tokens[tokens.length - 1], suffix = (tokens.length == 1) ? "" : "(" + tokens[0] + ")";
      String originalColumn;
      throw new RuntimeException("not supported."); // added (in place of code that was no longer compilable). --Ofer Givoli
    }
  }

}
