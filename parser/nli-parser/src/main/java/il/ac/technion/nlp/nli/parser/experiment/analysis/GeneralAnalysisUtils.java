package il.ac.technion.nlp.nli.parser.experiment.analysis;

import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.Formula;

import java.util.regex.Pattern;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class GeneralAnalysisUtils {

    public static String getHumanFriendlyRepresentationOfFormula(Formula formula){
        return formula.toString()
                .replaceAll("[a-zA-Z0-9_.:]*\\.", "") // erasing label prefixes ending with "."
                .replaceFirst("^\\(","")
                .replaceFirst("\\)$","")
                .replaceAll(Pattern.quote("(argmax (number 1) (number 1) "), "(argmax ")
                .replaceAll(Pattern.quote("(argmin (number 1) (number 1) "), "(argmin ");
    }



    /**
     * @return All the features (including regular Sempre features) of this derivation, including its descendants
     * derivations. [key,val] = [full feature name, feature value].
     */
    public static SafeMap<String,Double> getAllFeaturesOfDeriv(Derivation deriv) {
        SafeMap<String,Double> map = new SafeHashMap<>();
        addAllFeaturesOfDeriv(deriv,map);
        return map;
    }

    /**
     * see {@link #getAllFeaturesOfDeriv(Derivation)}
     */
    private static void addAllFeaturesOfDeriv(Derivation deriv, SafeMap<String, Double> map) {
        deriv.getChildren().forEach(c->addAllFeaturesOfDeriv(c,map));
        deriv.getLocalFeatureVector().toMap().forEach((key,val)-> {
            if (map.safeContainsKey(key))
                map.put(key, map.safeGet(key)+val);
            else
                map.put(key, val);
        });
    }

}
