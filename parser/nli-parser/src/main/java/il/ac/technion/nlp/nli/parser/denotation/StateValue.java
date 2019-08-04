package il.ac.technion.nlp.nli.parser.denotation;

import edu.stanford.nlp.sempre.Value;
import fig.basic.LispTree;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.parser.NliMethodCallFormula;
import org.jetbrains.annotations.Nullable;

/**
 * This is the denotation of a {@link NliMethodCallFormula} .
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public abstract class StateValue extends Value {



    /**
     * @return null represents an undefined state, because the {@link MethodCall}
     * execution failed (see {@link MethodCall#invokeOnDeepCopyOfState(State)}).
     */
    public abstract @Nullable State getState();

    @Override
    public LispTree toLispTree() {
        String humanFriendlyLossyDescription = getState() == null ? "[invalid state]" : "[valid state]";
        return LispTree.proto.newList().addChild(humanFriendlyLossyDescription);
    }

    @Override
    public String toString() {
        return getState() == null ? "[invalid state]" : getState().toString();
    }


}
