package il.ac.technion.nlp.nli.parser.general;

import edu.stanford.nlp.sempre.Derivation;
import edu.stanford.nlp.sempre.Example;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class Utils {

    public static  boolean isWindowsOS() {
        return System.getProperty("os.name").startsWith("Windows");
    }


    public static boolean derivationIsRoot(Example ex, Derivation deriv) {
        return deriv.isRoot(ex.numTokens());
    }
}
