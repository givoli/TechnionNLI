package il.ac.technion.nlp.nli.parser.features;

import com.google.common.collect.HashMultiset;
import edu.stanford.nlp.sempre.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneralFeatureUtils {

    /**
     * Returns all the {@link Value} nodes that are in the formula tree of 'deriv' but not in one of its children.
     */
    public static List<Value> getLocalPrimitiveValuesOfDerivation(Derivation deriv, boolean deterministic){
        HashMultiset<Value> valuesInChildren = HashMultiset.create();
        deriv.children.forEach(child->
                valuesInChildren.addAll(getAllPrimitiveValuesInFormula(child.formula)));

        List<Value> result = getAllPrimitiveValuesInFormula(deriv.formula);
        result.removeAll(valuesInChildren);
        Stream<Value> stream = result.stream();
        if (deterministic)
            stream = stream.sorted();
        return stream.collect(Collectors.toList());
    }

    /**
     * @return the number of {@link Formula} objects in the logical form tree defined by 'rootFormula'.
     */
    public static int calculateFormulaSize(Formula rootFormula){
        return getAllFormulaNodes(rootFormula).size();
    }

    public static List<Value> getAllPrimitiveValuesInFormula(Formula formula){
        return getAllFormulaNodes(formula).stream()
                .filter(f->f instanceof ValueFormula)
                .map(f->((ValueFormula)f).value)
                .collect(Collectors.toList());
    }


    /**
     * @return the nodes in the logical form tree defined by 'rootFormula'.
     */
    private static List<Formula> getAllFormulaNodes(Formula rootFormula){
        List<Formula> result = new LinkedList<>();
        List<Formula> unused = new LinkedList<>();
        rootFormula.mapToList(tmp->{
                result.add(tmp);
                return unused;}, true);
        return result;
    }

}
