//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre;

import edu.stanford.nlp.sempre.tables.features.PredicateInfo;
import fig.basic.*;
import il.ac.technion.nlp.nli.parser.features.phrase_predicate.PhrasePredicateAlignments;

import java.util.*;

/**
 * A Derivation corresponds to the production of a (partial) logical form
 * |formula| from a span of the utterance [start, end). Contains the formula and
 * what was used to produce it (like a search state). Each derivation is created
 * by a grammar rule and has some features and a score.
 *
 * @author Percy Liang
 */
public class Derivation implements SemanticFn.Callable, HasScore {
  public static class Options {
    @Option(gloss = "When printing derivations, to show values (could be quite verbose)")
    public boolean showValues = true;
    @Option(gloss = "When printing derivations, to show the first value (ignored when showValues is set)")
    public boolean showFirstValue = false;
    @Option(gloss = "When printing derivations, to show types")
    public boolean showTypes = true;
    @Option(gloss = "When printing derivations, to show rules")
    public boolean showRules = false;
    @Option(gloss = "When printing derivations, to show canonical utterance")
    public boolean showUtterance = false;
    @Option(gloss = "When executing, show formulae (for debugging)")
    public boolean showExecutions = false;
  }

  public static Options opts = new Options();

  //// Basic fields: created by the constructor.

  // Span that the derivation is built over
  public final String cat;
  public final int start;
  public final int end;

  // Floating cell information
  // TODO(yushi): make fields final
  public String canonicalUtterance;
  private boolean[] anchoredTokens;   // Tokens which anchored rules are defined on

  // If this derivation is composed of other derivations
  public final Rule rule;  // Which rule was used to produce this derivation?  Set to nullRule if not.
  public final List<Derivation> children;  // Corresponds to the RHS of the rule.

  //// SemanticFn fields: read/written by SemanticFn.
  // Note: SemanticFn should only depend on Formula and the Freebase type
  // information.  This could be its own class, but expose more right now to
  // be more flexible.

  public final Formula formula; // Logical form produced by this derivation
  public final SemType type; // Type corresponding to that logical form

  //// Fields produced by feature extractor, evaluation, etc.

  private List<String> localChoices;  // Just for printing/debugging.

  // TODO(pliang): make fields private

  // Information for scoring
  private final FeatureVector localFeatureVector;  // Features
  double score = Double.NaN;  // Weighted combination of features

  // Used during parsing (by FeatureExtractor, SemanticFn) to cache arbitrary
  // computation across different sub-Derivations.
  // Convention:
  // - use the featureDomain, FeatureComputer or SemanticFn as the key.
  // - the value is whatever the FeatureExtractor needs.
  // This information should be set to null after parsing is done.
  private Map<String, Object> tempState;

  /**
   * Added this field. --Ofer Givoli
   * We're updating to this data structure whenever we extract a new local unlexicalized feature from this derivation.
   * The purpose is to support the extraction of features indicating facts about unlexicalized features with different
   * {@link PredicateInfo.PredicateType}s being extracted for the same utterance phrase.
   */
  public final PhrasePredicateAlignments localPhrasePredicateAlignments = new PhrasePredicateAlignments();

  // What the formula evaluates to (optionally set later; only non-null for the root Derivation)
  public Value value;
  public Evaluation executorStats;

  // Number in [0, 1] denoting how correct the value is.
  public double compatibility = Double.NaN;
  // Probability (normalized exp of score).
  public double prob = Double.NaN;

  // Miscellaneous statistics
  int maxBeamPosition = -1;  // Lowest position that this tree or any of its children is on the beam (after sorting)
  int maxUnsortedBeamPosition = -1;  // Lowest position that this tree or any of its children is on the beam (before sorting)
  int preSortBeamPosition = -1;
  int postSortBeamPosition = -1;

  // Cache the hash code
  int hashCode = -1;

  // Each derivation that gets created gets a unique ID in increasing order so that
  // we can break ties consistently for reproducible results.
  long creationIndex;
  public static long numCreated = 0;  // Incremented for each derivation we create.
  public static final Comparator<Derivation> derivScoreComparator = new ScoredDerivationComparator();

  public static final List<Derivation> emptyList = Collections.emptyList();

  // A Derivation is built from

  /** Builder for everyone. */
  public static class Builder {
    private String cat;
    private int start;
    private int end;
    private Rule rule;
    private List<Derivation> children;
    private Formula formula;
    private SemType type;
    private FeatureVector localFeatureVector = new FeatureVector();
    private double score = Double.NaN;
    private Value value;
    private Evaluation executorStats;
    private double compatibility = Double.NaN;
    private double prob = Double.NaN;
    private String canonicalUtterance = "";

    public Builder cat(String cat) { this.cat = cat; return this; }
    public Builder start(int start) { this.start = start; return this; }
    public Builder end(int end) { this.end = end; return this; }
    public Builder rule(Rule rule) { this.rule = rule; return this; }
    public Builder children(List<Derivation> children) { this.children = children; return this; }
    public Builder formula(Formula formula) { this.formula = formula; return this; }
    public Builder type(SemType type) { this.type = type; return this; }
    public Builder localFeatureVector(FeatureVector localFeatureVector) { this.localFeatureVector = localFeatureVector; return this; }
    public Builder score(double score) { this.score = score; return this; }
    public Builder value(Value value) { this.value = value; return this; }
    public Builder executorStats(Evaluation executorStats) { this.executorStats = executorStats; return this; }
    public Builder compatibility(double compatibility) { this.compatibility = compatibility; return this; }
    public Builder prob(double prob) { this.prob = prob; return this; }
    public Builder canonicalUtterance(String canonicalUtterance) { this.canonicalUtterance = canonicalUtterance; return this; }

    public Builder withStringFormulaFrom(String value) {
      this.formula = new ValueFormula<>(new StringValue(value));
      this.type = SemType.stringType;
      return this;
    }
    public Builder withFormulaFrom(Derivation deriv) {
      this.formula = deriv.formula;
      this.type = deriv.type;
      return this;
    }

    public Builder withCallable(SemanticFn.Callable c) {
      this.cat = c.getCat();
      this.start = c.getStart();
      this.end = c.getEnd();
      this.rule = c.getRule();
      this.children = c.getChildren();
      return this;
    }

    public Derivation createDerivation() {
      return new Derivation(
          cat, start, end, rule, children, formula, type,
          localFeatureVector, score, value, executorStats, compatibility, prob,
          canonicalUtterance);
    }
  }

  // Changed to public. --Ofer Givoli
  public Derivation(String cat, int start, int end, Rule rule, List<Derivation> children, Formula formula, SemType type,
      FeatureVector localFeatureVector, double score, Value value, Evaluation executorStats, double compatibility, double prob,
      String canonicalUtterance) {
    this.cat = cat;
    this.start = start;
    this.end = end;
    this.rule = rule;
    this.children = children;
    this.formula = formula;
    this.type = type;
    this.localFeatureVector = localFeatureVector;
    this.score = score;
    this.value = value;
    this.executorStats = executorStats;
    this.compatibility = compatibility;
    this.prob = prob;
    this.canonicalUtterance = canonicalUtterance;
    this.creationIndex = numCreated++;
  }

  public Formula getFormula() { return formula; }
  public double getScore() { return score; }
  public double getProb() { return prob; }
  public double getCompatibility() { return compatibility; }
  public List<Derivation> getChildren() { return children; }
  public Value getValue() { return value; }

  public boolean isFeaturizedAndScored() { return !Double.isNaN(score); }
  public boolean isExecuted() { return value != null; }
  public int getMaxBeamPosition() { return maxBeamPosition; }
  public String getCat() { return cat; }
  public int getStart() { return start; }
  public int getEnd() { return end; }
  public boolean containsIndex(int i) { return i < end && i >= start; }
  public Rule getRule() { return rule; }
  public Evaluation getExecutorStats() { return executorStats; }
  public FeatureVector getLocalFeatureVector() { return localFeatureVector; }

  public Derivation child(int i) { return children.get(i); }
  public String childStringValue(int i) {
    return Formulas.getString(children.get(i).formula);
  }

  // Return whether |deriv| is built over the root Derivation.
  public boolean isRoot(int numTokens) {
    return cat.equals(Rule.rootCat) && ((start == 0 && end == numTokens) || (start == -1));
  }

  // Return whether |deriv| has root category (for floating parser)
  public boolean isRootCat() {
    return cat.equals(Rule.rootCat);
  }

  // Functions that operate on features.
  public void addFeature(String domain, String name) { addFeature(domain, name, 1); }
  public void addFeature(String domain, String name, double value) { this.localFeatureVector.add(domain, name, value); }
  public void addHistogramFeature(String domain, String name, double value,
                                  int initBinSize, int numBins, boolean exp) {
    this.localFeatureVector.addHistogram(domain, name, value, initBinSize, numBins, exp);
  }
  public void addFeatureWithBias(String domain, String name, double value) { this.localFeatureVector.addWithBias(domain, name, value); }
  public void addFeatures(FeatureVector fv) { this.localFeatureVector.add(fv); }

  public double localScore(Params params) {
    return localFeatureVector.dotProduct(params);
  }

  /**
   * Recursively compute the score for each node in derivation. Update |score|
   * field as well as return its value.
   */
  public double computeScore(Params params) {
    score = localScore(params);
    if (children != null)
      for (Derivation child : children)
        score += child.computeScore(params);
    return score;
  }

  /**
   * Same as |computeScore()| but without recursion (assumes children are
   * already scored).
   */
  public double computeScoreLocal(Params params) {
    score = localScore(params);
    if (children != null)
      for (Derivation child : children)
        score += child.score;
    return score;
  }

  // If we haven't executed the formula associated with this derivation, then
  // execute it!
  public void ensureExecuted(Executor executor, ContextValue context) {
    if (isExecuted()) return;
    StopWatchSet.begin("Executor.execute");
    if (opts.showExecutions)
      LogInfo.logs("%s - %s", canonicalUtterance, formula);
    Executor.Response response = executor.execute(formula, context);
    StopWatchSet.end();
    value = response.value;
    executorStats = response.stats;
  }

  public LispTree toLispTree() {
    LispTree tree = LispTree.proto.newList();
    tree.addChild("derivation");
    if (formula != null)
      tree.addChild(LispTree.proto.newList("formula", formula.toLispTree()));
    if (value != null) {
      if (opts.showValues)
        tree.addChild(LispTree.proto.newList("value", value.toLispTree()));
      else if (value instanceof ListValue) {
        List<Value> values = ((ListValue) value).values;
        if (opts.showFirstValue && values.size() > 0) {
          tree.addChild(LispTree.proto.newList(values.size() + " values", values.get(0).toLispTree()));
        } else {
          tree.addChild(values.size() + " values");
        }
      }

    }
    if (type != null && opts.showTypes)
      tree.addChild(LispTree.proto.newList("type", type.toLispTree()));
    if (opts.showRules) {
      if (rule != null) tree.addChild(getRuleLispTree());
    }
    if (opts.showUtterance && canonicalUtterance != null) {
      tree.addChild(LispTree.proto.newList("canonicalUtterance", canonicalUtterance));
    }
    return tree;
  }

  /**
   * @return lisp tree showing the entire parse tree
   */
  public LispTree toRecursiveLispTree() {
    LispTree tree = LispTree.proto.newList();
    tree.addChild("derivation");
    tree.addChild(LispTree.proto.newList("span", cat + "[" + start + ":" + end + "]"));
    if (formula != null)
      tree.addChild(LispTree.proto.newList("formula", formula.toLispTree()));
    for (Derivation child : children)
      tree.addChild(child.toRecursiveLispTree());
    return tree;
  }

  public String toRecursiveString() {
    return toRecursiveLispTree().toString();
  }

  // TODO(pliang): remove this in favor of localChoices
  private LispTree getRuleLispTree() {
    LispTree tree = LispTree.proto.newList();
    tree.addChild("rules");
    getRuleLispTreeRecurs(tree);
    return tree;
  }
  private void getRuleLispTreeRecurs(LispTree tree) {
    if (children.size() > 0) {
      tree.addChild(LispTree.proto.newList("rule", rule.toLispTree()));
      for (Derivation child : children) {
        child.getRuleLispTreeRecurs(tree);
      }
    }
  }

  public String startEndString(List<String> tokens) {
    return start + ":" + end + (start == -1 ? "" : tokens.subList(start, end));
  }
  public String toString() { return toLispTree().toString(); }

  public void incrementLocalFeatureVector(double factor, Map<String, Double> map) {
    localFeatureVector.increment(factor, map, AllFeatureMatcher.matcher);
  }
  public void incrementAllFeatureVector(double factor, Map<String, Double> map) {
    incrementAllFeatureVector(factor, map, AllFeatureMatcher.matcher);
  }
  public void incrementAllFeatureVector(double factor, Map<String, Double> map, FeatureMatcher updateFeatureMatcher) {
    localFeatureVector.increment(factor, map, updateFeatureMatcher);
    for (Derivation child : children)
      child.incrementAllFeatureVector(factor, map, updateFeatureMatcher);
  }
  public void incrementAllFeatureVector(double factor, FeatureVector fv) {
    localFeatureVector.add(factor, fv);
    for (Derivation child : children)
      child.incrementAllFeatureVector(factor, fv);
  }

  // returns feature vector with renamed features by prefix
  public FeatureVector addPrefixLocalFeatureVector(String prefix) {
    return localFeatureVector.addPrefix(prefix);
  }

  public Map<String, Double> getAllFeatureVector() {
    Map<String, Double> m = new HashMap<>();
    incrementAllFeatureVector(1.0d, m, AllFeatureMatcher.matcher);
    return m;
  }

  // TODO(pliang): this is crazy inefficient
  public double getAllFeatureVector(String featureName) {
    Map<String, Double> m = new HashMap<>();
    incrementAllFeatureVector(1.0d, m, new ExactFeatureMatcher(featureName));
    return MapUtils.get(m, featureName, 0.0);
  }

  public void addLocalChoice(String choice) {
    if (localChoices == null)
      localChoices = new ArrayList<String>();
    localChoices.add(choice);
  }

  public void incrementAllChoices(int factor, Map<String, Integer> map) {
    if (opts.showRules)
      MapUtils.incr(map, "[" + start + ":" + end + "] " + rule.toString(), 1);
    if (localChoices != null) {
      for (String choice : localChoices)
        MapUtils.incr(map, choice, factor);
    }
    for (Derivation child : children)
      child.incrementAllChoices(factor, map);
  }

  // Used to compare derivations by score.
  public static class ScoredDerivationComparator implements Comparator<Derivation> {
    @Override
    public int compare(Derivation deriv1, Derivation deriv2) {
      if (deriv1.score > deriv2.score) return -1;
      if (deriv1.score < deriv2.score) return +1;
      // Ensure reproducible randomness
      if (deriv1.creationIndex < deriv2.creationIndex) return -1;
      if (deriv1.creationIndex > deriv2.creationIndex) return +1;
      return 0;
    }
  }

  // Used to compare derivations by compatibility.
  public static class CompatibilityDerivationComparator implements Comparator<Derivation> {
    @Override
    public int compare(Derivation deriv1, Derivation deriv2) {
      if (deriv1.compatibility > deriv2.compatibility) return -1;
      if (deriv1.compatibility < deriv2.compatibility) return +1;
      // Ensure reproducible randomness
      if (deriv1.creationIndex < deriv2.creationIndex) return -1;
      if (deriv1.creationIndex > deriv2.creationIndex) return +1;
      return 0;
    }
  }

  // for debugging
  public void printDerivationRecursively() {
    LogInfo.logs("Deriv: %s(%s,%s) %s", cat, start, end, formula);
    for (int i = 0; i < children.size(); i++) {
      LogInfo.begin_track("child %s:", i);
      children.get(i).printDerivationRecursively();
      LogInfo.end_track();
    }
  }

  public static void sortByScore(List<Derivation> trees) {
    Collections.sort(trees, derivScoreComparator);
  }

  // Generate a probability distribution over derivations given their scores.
  public static double[] getProbs(List<Derivation> derivations, double temperature) {
    double[] probs = new double[derivations.size()];
    for (int i = 0; i < derivations.size(); i++)
      probs[i] = derivations.get(i).getScore() / temperature;
    if (probs.length > 0)
      NumUtils.expNormalize(probs);
    return probs;
  }

  // Manipulation of temporary state used during parsing.
  public Map<String, Object> getTempState() {
    // Create the tempState if it doesn't exist.
    if (tempState == null)
      tempState = new HashMap<String, Object>();
    return tempState;
  }
  public void clearTempState() {
    tempState = null;
    if (children != null)
      for (Derivation child : children)
        child.clearTempState();
  }

  // Compute anchoredTokens and return the result
  // anchoredTokens[>= anchoredTokens.length] are False by default
  public boolean[] getAnchoredTokens() {
    if (anchoredTokens == null) {
      if (rule.isAnchored()) {
        anchoredTokens = new boolean[end];
        for (int i = start; i < end; i++) anchoredTokens[i] = true;
      } else {
        anchoredTokens = new boolean[0];
        for (Derivation child : children) {
          boolean[] childAnchoredTokens = child.getAnchoredTokens();
          if (anchoredTokens.length < childAnchoredTokens.length) {
            boolean[] newAnchoredTokens = new boolean[childAnchoredTokens.length];
            for (int i = 0; i < anchoredTokens.length; i++) newAnchoredTokens[i] = anchoredTokens[i];
            anchoredTokens = newAnchoredTokens;
          }
          for (int i = 0; i < childAnchoredTokens.length; i++)
            anchoredTokens[i] = anchoredTokens[i] || childAnchoredTokens[i];
        }
      }
    }
    return anchoredTokens.clone();
  }
}
