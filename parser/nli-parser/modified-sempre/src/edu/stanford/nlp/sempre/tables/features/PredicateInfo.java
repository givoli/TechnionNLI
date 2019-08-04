//NOTICE: this file was modified by Ofer Givoli (i.e. it's not identical to the matching file in the original Sempre package).
package edu.stanford.nlp.sempre.tables.features;

import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.tables.TableTypeSystem;
import edu.stanford.nlp.sempre.tables.lambdadcs.InfiniteUnaryDenotation;
import fig.basic.LispTree;
import fig.basic.Option;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.NliMethodCallFormula;
import il.ac.technion.nlp.nli.parser.NameValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

// Modified this file. --Ofer Givoli

/**
 * Modified the following documentation. --Ofer Givoli
 * Represents some fact about a {@link Formula}, to be used for feature extraction.
 * Most commonly, the represented fact is that the logical form tree defined by the {@link Formula} contains a
 * specific node (originally referred to as "predicate", hence the class name).
 *
 * @author ppasupat
 */
public class PredicateInfo {

  public static final String LAMBDA_PREDICATE_ID = "lambda"; // added. --Ofer Givoli

  public static class Options {
    @Option(gloss = "Use lemma form of original strings (need LanguageAnalyzer -- slow)")
    public boolean usePredicateLemma = false;
    @Option(gloss = "Conversion between (reverse [NameValue]) and ![NameValue]")
    public ReverseNameValueConversion reverseNameValueConversion = ReverseNameValueConversion.none;
    @Option(gloss = "Allow repreated predicates")
    public boolean allowRepeats = false;
    @Option(gloss = "Maximum length of predicate string")
    public int maxPredicateLength = 40;
    @Option(gloss = "Perform beta reduction before finding predicates")
    public boolean betaReduce = false;
  }
  public static Options opts = new Options();

  public static enum ReverseNameValueConversion { allBang, allReverse, none }; // changed to public. --Ofer Givoli

  //changed to public, renamed 'BINARY' to 'RELATION' and added the constants that follows the first line. --Ofer Givoli
  public static enum PredicateType { KEYWORD, ENTITY, RELATION,
    NLI_ENTITY_TYPE, NLI_METHOD_NAME,
    /**
     * A {@link PredicateInfo} of this type doesn't correspond to any real logical form node, but
     * rather is used to represent a sub-feature that is to be concatenated with other (phrase related) sub-features to
     * produce entire features.
     */
    SUBFEATURE
  }

  public final @Nullable Value value;
  /**
   * Null iff {@link #value} is not null. When not null, should be a human-friendly string.
   */
  public final @Nullable String idForNonValue;
  /**
   * Documentation added. --Ofer Givoli
   * Not used by the NLI system.
   */
  public final PredicateType type;


  /**
   * @param nameValuesManager used to get the {@link PredicateType} of 'value' (a reference to this argument isn't
   *                          kept).
   */
  public PredicateInfo(NameValuesManager nameValuesManager, @NotNull Value value) { //Added. --Ofer Givoli
    this.value = value;
    this.idForNonValue = null;
    this.type = getPredicateType(nameValuesManager, value);
  }

  public PredicateInfo(@NotNull Value value, PredicateType type) { //Added. --Ofer Givoli
    this.value = value;
    this.idForNonValue = null;
    this.type = type;
  }

  public PredicateInfo(@NotNull String idForNonValue, PredicateType type) { //Added. --Ofer Givoli
    this.value = null;
    this.idForNonValue = idForNonValue;
    this.type = type;
  }


  private static PredicateType getPredicateType(NameValuesManager nameValuesManager, Value value){  // Added. --Ofer Givoli

    if (!(value instanceof NameValue))
      return PredicateType.ENTITY;

    NameValue nameValue = (NameValue) value;
    NameValuesManager.NameValueType nameValueType = nameValuesManager.getNameValueType(nameValue);
    if (nameValueType == null) {
      if (InfiniteUnaryDenotation.ComparisonUnaryDenotation.COMPARATORS.contains(nameValue.id))
        return PredicateType.RELATION;
      throw new RuntimeException("Unrecognized NameValue: " + nameValue.id);
    }

    if (nameValueType == NameValuesManager.NameValueType.NLI_METHOD_NAME)
      return PredicateType.NLI_METHOD_NAME;
    else if (nameValueType == NameValuesManager.NameValueType.ENUM ||
            nameValueType == NameValuesManager.NameValueType.NLI_ENTITY ||
            nameValueType == NameValuesManager.NameValueType.STRING)
      return PredicateType.ENTITY;
    else if (nameValueType == NameValuesManager.NameValueType.FIELD_RELATION ||
            nameValueType == NameValuesManager.NameValueType.NON_FIELD_RELATION)
      return PredicateType.RELATION;
    else if (nameValueType == NameValuesManager.NameValueType.NLI_ENTITY_TYPE)
      return PredicateType.NLI_ENTITY_TYPE;
        else
    throw new Error("unsupported type");

  }


  @Override
  public String toString() {
      //modified method. --Ofer Givoli
    return value == null ? "id=" + idForNonValue : "value=" + value;
  }

  /**
   * Modified. --Ofer Givoli
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PredicateInfo that = (PredicateInfo) o;
    return Objects.equals(value, that.value) &&
            Objects.equals(idForNonValue, that.idForNonValue);
  }

  /**
   * Modified. --Ofer Givoli
   */
  @Override
  public int hashCode() {

    return Objects.hash(value, idForNonValue);
  }

// ============================================================
  // Get original strings and lemmas
  // ============================================================

  // Lemma cache
  private static final Map<String, String> lemmaCache = new HashMap<>();

  // Helper function: get lemma form
  public static synchronized String getLemma(String s) {
    if (s == null || s.trim().isEmpty()) return null;
    String lemma = lemmaCache.get(s);
    if (lemma == null) {
      LanguageInfo langInfo = LanguageAnalyzer.getSingleton().analyze(s);
      lemma = (langInfo.numTokens() == 0) ? "" : langInfo.lemmaPhrase(0, langInfo.numTokens());
      lemmaCache.put(s, lemma);
    }
    return lemma;
  }


  // ============================================================
  // Get the list of all PredicateInfos
  // ============================================================

  public static List<PredicateInfo> getPredicateInfos(Derivation deriv) {
    // modified method. --Ofer Givoli
    Formula formula = deriv.formula;
    formula = getFormulaAfterPredicateExtractionPrepossessing(formula); // Modified (factored out). --Ofer Givoli

    // Traverse on formula. Be more careful when generating predicates
    FormulaTraverser traverser = new FormulaTraverser();
    traverser.traverse(formula);
    return new ArrayList<>(traverser.predicates);
  }

  // Added. --Ofer Givoli
  public static Formula getFormulaAfterPredicateExtractionPrepossessing(Formula formula) {
    return opts.betaReduce ? Formulas.betaReduction(formula) : formula;
  }


  private static class FormulaTraverser {
    public final Collection<PredicateInfo> predicates;

    public FormulaTraverser() { // Removed argument. --Ofer Givoli
      this.predicates = opts.allowRepeats ? new ArrayList<>() : new HashSet<>();
    }

    public void traverse(Formula formula) {
      // modified method. --Ofer Givoli
      if (formula instanceof ValueFormula) {
        Value value = ((ValueFormula) formula).value;

        if (!ExperimentRunner.isExperimentCurrentlyRunning())
          throw new RuntimeException("not supported");

        if (!(value instanceof StringValue)) // StringValue values, unlike NameValue objects that represents phrases, are not useful here. --Ofer Givoli
          predicates.add(new PredicateInfo(ExperimentRunner.getCurrentExperiment().getCurrentInferenceData().graph
                  .nameValuesManager, value));

        if (value instanceof NumberValue) {
          predicates.add(new PredicateInfo("number", PredicateType.KEYWORD));
        } else if (value instanceof DateValue) {
          predicates.add(new PredicateInfo("date", PredicateType.KEYWORD));
        } else if (value instanceof TimeValue) {
          predicates.add(new PredicateInfo("time", PredicateType.KEYWORD));
        } else if (!(value instanceof NameValue) && !(value instanceof StringValue))
          throw new Error("unsupported type");
      } else if (formula instanceof JoinFormula) {
        JoinFormula join = (JoinFormula) formula;
        traverse(join.relation); traverse(join.child);

      } else if (formula instanceof ReverseFormula) {
        // changed this logic. --Ofer Givoli
        predicates.add(new PredicateInfo("reverse", PredicateType.KEYWORD));
        ReverseFormula reverse = (ReverseFormula) formula;
        if (reverse.child instanceof ValueFormula
                && ((ValueFormula) reverse.child).value instanceof NameValue) {
          String id = NameValuesManager.getNonReversedNameValueId(((NameValue) ((ValueFormula) reverse.child).value)
                  .id);
          traverse(new ValueFormula<>(new NameValue("!" + id)));
        }
        traverse(reverse.child);
      } else if (formula instanceof MergeFormula) {
        MergeFormula merge = (MergeFormula) formula;
        predicates.add(new PredicateInfo(merge.mode.toString(), PredicateType.KEYWORD));
        traverse(merge.child1); traverse(merge.child2);

      } else if (formula instanceof AggregateFormula) {
        AggregateFormula aggregate = (AggregateFormula) formula;
        predicates.add(new PredicateInfo(aggregate.mode.toString(), PredicateType.KEYWORD));
        traverse(aggregate.child);

      } else if (formula instanceof SuperlativeFormula) {
        SuperlativeFormula superlative = (SuperlativeFormula) formula;
        predicates.add(new PredicateInfo(superlative.mode.toString(), PredicateType.KEYWORD));
        // Skip the "(number 1) (number 1)" part
        traverse(superlative.head); traverse(superlative.relation);

      } else if (formula instanceof ArithmeticFormula) {
        ArithmeticFormula arithmetic = (ArithmeticFormula) formula;
        predicates.add(new PredicateInfo(arithmetic.mode.toString(), PredicateType.KEYWORD));
        traverse(arithmetic.child1); traverse(arithmetic.child2);

      } else if (formula instanceof VariableFormula) {
        // Skip variables

      } else if (formula instanceof MarkFormula) {
        MarkFormula mark = (MarkFormula) formula;
        predicates.add(new PredicateInfo("mark", PredicateType.KEYWORD));
        // Skip variable
        traverse(mark.body);

      } else if (formula instanceof LambdaFormula) {
        LambdaFormula lambda = (LambdaFormula) formula;
        // modified and commented out the following line. --Ofer Givoli
//        predicates.add(new PredicateInfo(LAMBDA_PREDICATE_ID, PredicateType.KEYWORD));
        // Skip variable
        traverse(lambda.body);

      } else if (formula instanceof NliMethodCallFormula) { //Added. --Ofer Givoli
        NliMethodCallFormula functionCall = (NliMethodCallFormula) formula;
        predicates.add(new PredicateInfo(NliMethodCallFormula.class.getSimpleName(), PredicateType.KEYWORD));
        functionCall.getAllStrictlySubFormulasContained().forEach(this::traverse);
      } else {
        throw new RuntimeException("[PredicateInfo] Cannot handle formula " + formula);
      }
    }
  }

  // ============================================================
  // Formula normalization
  // ============================================================

  public static LispTree normalizeFormula(Example ex, Derivation deriv) {
    Formula formula = deriv.formula;
    if (opts.betaReduce) formula = Formulas.betaReduction(formula);
    return new FormulaNormalizer(ex, formula).getNormalizedLispTree();
  }

  private static class FormulaNormalizer {
    private final ContextValue context;
    private final Formula formula;
    private LispTree normalized;
    private Map<String, String> foundFields;

    public FormulaNormalizer(Example ex, Formula formula) {
      this.context = ex.context;
      this.formula = formula;
      foundFields = new HashMap<>();
    }

    private String getNormalizedPredicate(String predicate) {
      if (ExperimentRunner.isExperimentCurrentlyRunning()) // Added if block. --Ofer Givoli
        throw new RuntimeException("not supported by the instructions parser");

      if (predicate.charAt(0) == '!') return "!" + getNormalizedPredicate(predicate.substring(1));
      if (predicate.equals(CanonicalNames.TYPE)) return "@type";
      if (predicate.equals(TableTypeSystem.ROW_TYPE)) return "@row";
      if (predicate.startsWith(TableTypeSystem.ROW_PROPERTY_NAME_PREFIX)) {
        String fieldname = TableTypeSystem.getIdAfterPeriod(predicate, TableTypeSystem.ROW_PROPERTY_NAME_PREFIX);
        if (predicate.equals(TableTypeSystem.ROW_NEXT_VALUE.id) || predicate.equals(TableTypeSystem.ROW_INDEX_VALUE.id))
          return "@" + fieldname;
        if (!foundFields.containsKey(fieldname)) foundFields.put(fieldname, "" + foundFields.size());
        return "r" + foundFields.get(fieldname);
      } else if (predicate.startsWith(TableTypeSystem.CELL_NAME_PREFIX)) {
        if (predicate.startsWith(TableTypeSystem.CELL_PROPERTY_NAME_PREFIX)) {
          return "@" + TableTypeSystem.getIdAfterPeriod(predicate, TableTypeSystem.CELL_PROPERTY_NAME_PREFIX);
        }
        String fieldname = TableTypeSystem.getIdAfterUnderscore(predicate, TableTypeSystem.CELL_NAME_PREFIX);
        if (!foundFields.containsKey(fieldname)) foundFields.put(fieldname, "" + foundFields.size());
        return "c" + foundFields.get(fieldname);
      }
      return predicate;
    }

    public LispTree getNormalizedLispTree() {
      if (normalized == null) normalized = traverse(formula);
      return normalized;
    }

    /** The only place the returned tree is used is in {@link edu.stanford.nlp.sempre.tables.features.PhrasePredicateFeatureComputer#extractPhraseFormula(edu.stanford.nlp.sempre.Example, edu.stanford.nlp.sempre.Derivation, java.util.List)}. Documentation added. --Ofer Givoli */
    public LispTree traverse(Formula formula) {
      LispTree tree = LispTree.proto.newList();

      if (formula instanceof ValueFormula) {
        Value value = ((ValueFormula) formula).value;
        if (value instanceof NumberValue) {
          NumberValue number = (NumberValue) value;
          return LispTree.proto.newLeaf("$number");

        } else if (value instanceof DateValue) {
          DateValue date = (DateValue) value;
          return LispTree.proto.newLeaf("$date");

        } else if (value instanceof StringValue) {
          StringValue string = (StringValue) value;
          return LispTree.proto.newLeaf("$string");

        } else if (value instanceof NameValue) {
          NameValue name = (NameValue) value;
          String id = name.id;
          if (opts.reverseNameValueConversion == ReverseNameValueConversion.allReverse
              && id.startsWith("!") && !id.equals("!=")) {
            tree.addChild(LispTree.proto.newLeaf("reverse"));
            id = id.substring(1);
            tree.addChild(getNormalizedPredicate(id));
          } else {
            return LispTree.proto.newLeaf(getNormalizedPredicate(id));
          }

        }

      } else if (formula instanceof JoinFormula) {
        JoinFormula join = (JoinFormula) formula;
        tree.addChild(traverse(join.relation)).addChild(traverse(join.child));

      } else if (formula instanceof ReverseFormula) {
        ReverseFormula reverse = (ReverseFormula) formula;
        if (opts.reverseNameValueConversion == ReverseNameValueConversion.allBang
            && reverse.child instanceof ValueFormula
            && ((ValueFormula) reverse.child).value instanceof NameValue) {
          String id = ((NameValue) ((ValueFormula) reverse.child).value).id;
          id = id.startsWith("!") ? id.substring(1) : ("!" + id);
          return LispTree.proto.newLeaf(getNormalizedPredicate(id));
        } else {
          tree.addChild(LispTree.proto.newLeaf("reverse")).addChild(traverse(reverse.child));
        }

      } else if (formula instanceof MergeFormula) {
        MergeFormula merge = (MergeFormula) formula;
        tree.addChild(merge.mode.toString()).addChild(traverse(merge.child1)).addChild(traverse(merge.child2));

      } else if (formula instanceof AggregateFormula) {
        AggregateFormula aggregate = (AggregateFormula) formula;
        tree.addChild(aggregate.mode.toString()).addChild(traverse(aggregate.child));

      } else if (formula instanceof SuperlativeFormula) {
        SuperlativeFormula superlative = (SuperlativeFormula) formula;
        tree.addChild(superlative.mode.toString()).addChild(traverse(superlative.head)).addChild(traverse(superlative.relation));

      } else if (formula instanceof ArithmeticFormula) {
        ArithmeticFormula arithmetic = (ArithmeticFormula) formula;
        tree.addChild(arithmetic.mode.toString()).addChild(traverse(arithmetic.child1)).addChild(traverse(arithmetic.child2));

      } else if (formula instanceof VariableFormula) {
        return LispTree.proto.newLeaf("$var");

      } else if (formula instanceof MarkFormula) {
        MarkFormula mark = (MarkFormula) formula;
        tree.addChild("mark").addChild(traverse(mark.body));

      } else if (formula instanceof LambdaFormula) {
        LambdaFormula lambda = (LambdaFormula) formula;
        tree.addChild("lambda").addChild(traverse(lambda.body));

      } else if (formula instanceof NliMethodCallFormula) { //Added. --Ofer Givoli
        NliMethodCallFormula functionCall = (NliMethodCallFormula) formula;
        LispTree lispTree = tree.addChild(NliMethodCallFormula.class.getSimpleName());
        for (Formula c : functionCall.getAllStrictlySubFormulasContained())
          lispTree.addChild(traverse(c));
      } else {
        throw new RuntimeException("[PredicateInfo] Cannot handle formula " + formula);
      }

      return tree;
    }
  }

  public PredicateInfo getCanonicalForm(){ // Added. --Ofer Givoli
    if (value == null || !(value instanceof NameValue))
      return this;
    NameValue nameValue = (NameValue) value;

    if (!CanonicalNames.isReverseProperty(nameValue.id))
      return this;

    return new PredicateInfo(nameValue.getNonReversedNameValue(), type);
  }
}
