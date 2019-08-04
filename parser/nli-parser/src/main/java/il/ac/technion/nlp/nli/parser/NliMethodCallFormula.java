package il.ac.technion.nlp.nli.parser;

import com.google.common.base.Function;
import com.google.common.base.Verify;
import edu.stanford.nlp.sempre.*;
import fig.basic.LispTree;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.denotation.DenotationUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A Formula representing a method call with sub-formulas defining the arguments of the function.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class NliMethodCallFormula extends Formula {

    /**
     * The type of the the derivation of this formula.
     */
    public final static SemType DERIVATION_TYPE = new AtomicSemType("fun_call");


    /**
     * At the end, this will be a ValueFormula containing a NameValue whose id is the the friendly id of the interface
     * method. But that might not yet fulfill before function application (macro) of a lambda expression is done by
     * JoinFn.
     */
    private final Formula friendlyIdOfNliMethod;



    /**
     * @see #getArgIncludingPerhapsObjToInvokeOn()
     */
    private final List<Formula> argIncludingPerhapsObjToInvokeOn;


    /**
     * The is the value of the first child of the Lisp tree defining the way this object should be constructed.
     */
    public final static String KEYWORD_IN_GRAMMAR_LISP_TREE = "createFunctionCall";
    private final static String NLI_METHOD_LISP_TREE_NODE_PREFIX = "NliMethod:";


    public NliMethodCallFormula(Formula friendlyIdOfNliMethod, List<Formula> argIncludingPerhapsObjToInvokeOn) {
        this.friendlyIdOfNliMethod = friendlyIdOfNliMethod;
        this.argIncludingPerhapsObjToInvokeOn = argIncludingPerhapsObjToInvokeOn;
    }


    /**
     *
     * @param tree child(1): NameValue whose id is the friendly method id.
     *             Each following child represents an element of {@link #argIncludingPerhapsObjToInvokeOn}.
     */
    public NliMethodCallFormula(LispTree tree) {

        Verify.verify(tree.child(0).value.equals(KEYWORD_IN_GRAMMAR_LISP_TREE));

        //noinspection unchecked
        friendlyIdOfNliMethod = Formulas.fromLispTree(tree.child(1));

        argIncludingPerhapsObjToInvokeOn = new LinkedList<>();
        for (int i=2; i<tree.children.size(); i++)
            argIncludingPerhapsObjToInvokeOn.add(Formulas.fromLispTree(tree.child(i)));
    }

    public LispTree toLispTree() {
        LispTree tree = LispTree.proto.newList();
        LispTree subTree = tree.addChild(NLI_METHOD_LISP_TREE_NODE_PREFIX + friendlyIdOfNliMethod);
        argIncludingPerhapsObjToInvokeOn.forEach(formula->subTree.addChild(formula.toLispTree()));
        return tree;
    }


    public Formula map(@SuppressWarnings("Guava") Function<Formula, Formula> func) {
        Formula result = func.apply(this);
        return result == null ?
                new NliMethodCallFormula(friendlyIdOfNliMethod.map(func), argIncludingPerhapsObjToInvokeOn.stream()
                    .map(formula->formula.map(func)).collect(Collectors.toList()))
                : result;
    }

    @Override
    public List<Formula> mapToList(@SuppressWarnings("Guava") Function<Formula, List<Formula>> func, boolean alwaysRecurse) {
        List<Formula> res = func.apply(this);
        //noinspection ConstantConditions
        if (res.isEmpty() || alwaysRecurse) {
            getAllStrictlySubFormulasContained().stream().flatMap(formula ->
                    formula.mapToList(func, alwaysRecurse).stream()).forEach(res::add);
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NliMethodCallFormula that = (NliMethodCallFormula) o;
        return Objects.equals(friendlyIdOfNliMethod, that.friendlyIdOfNliMethod) &&
                Objects.equals(getArgIncludingPerhapsObjToInvokeOn(), that.getArgIncludingPerhapsObjToInvokeOn());
    }

    @Override
    public int computeHashCode() {
        return Objects.hash(friendlyIdOfNliMethod, argIncludingPerhapsObjToInvokeOn);
    }

    /**
     * Returns all the sub-formulas that define this formula, excluding this formula itself.
     */
    public List<Formula> getAllStrictlySubFormulasContained() {
        List<Formula> children = new LinkedList<>();
        children.add(friendlyIdOfNliMethod);
        children.addAll(argIncludingPerhapsObjToInvokeOn);
        return children;
    }

    /**
     * This method may only be called when the {@link NliMethodCallFormula} contains an explicit
     * {@link #friendlyIdOfNliMethod} as a {@link ValueFormula}.
     */
    public String getFriendlyIdOfMethod() {
        if (!(friendlyIdOfNliMethod instanceof ValueFormula))
            throw new RuntimeException("It seems that this NliMethodCallFormula is the formula field of ConstantFn, and thus contains LambdaFormula etc. This method is not supported in such cases.");

        ValueFormula valueFormula = (ValueFormula) friendlyIdOfNliMethod;
        if (!(valueFormula.value instanceof NameValue))
            throw new RuntimeException("Expected 'friendlyIdOfNliMethod' to hold a NameValue");

        NameValue nameValue = (NameValue) valueFormula.value;
        return ExperimentRunner.getCurrentExperiment().getCurrentInferenceData().graph.nameValuesManager
                .getNliMethodFriendlyId(nameValue);
    }

    /**
     * Each element represents a Formula whose denotation is one of the arguments of the interface method (according to the
     * order of the function's parameters).
     * The first argument should be a denote a singleton containing the object on which the NLI method is to be invoked
     * on if and only if {@link DenotationUtils#firstArgumentIsObjToInvokeMethodOn} returns true for it.
     */
    public List<Formula> getArgIncludingPerhapsObjToInvokeOn() {
        return argIncludingPerhapsObjToInvokeOn;
    }

}
