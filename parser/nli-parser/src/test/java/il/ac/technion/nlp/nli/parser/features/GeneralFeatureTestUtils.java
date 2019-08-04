package il.ac.technion.nlp.nli.parser.features;

import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.FeatureVector;
import edu.stanford.nlp.sempre.Formula;

import java.util.LinkedList;

public class GeneralFeatureTestUtils {


    public static Derivation createDerivation(Formula formula) {
        return new Derivation(null, 0,1, null, new LinkedList<>(), formula, null, new FeatureVector(), 0, null, null, 0, 0,
                null);
    }

    public static Derivation createDerivation() {
        return createDerivation(null);
    }


}
