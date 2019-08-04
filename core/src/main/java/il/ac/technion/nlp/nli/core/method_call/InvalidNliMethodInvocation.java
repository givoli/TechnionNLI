package il.ac.technion.nlp.nli.core.method_call;

/**
 * Exception to be thrown from within an NLI function to indicate that the function call (function +
 * arguments) is invalid (e.g. the arguments make no sense in the context of the initial states).
 *
 * Note: the developer user of this framework has the right to make their interface methods throw any {@link Throwable} and
 * the effect will be the same, but we ourselves should throw {@link InvalidNliMethodInvocation} when relevant (and the
 * developer user is advised to do so as well), because in that case no warnings will need to be logged.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InvalidNliMethodInvocation extends RuntimeException {

    public InvalidNliMethodInvocation() {
    }

    private static final long serialVersionUID = -10119665588739958L;
}
