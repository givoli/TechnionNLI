package il.ac.technion.nlp.nli.parser.denotation;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.state.State;

import java.util.Objects;

/**
 * The state represented is the result of executing a {@link MethodCall}, which is done lazily (and memoized).
 *
 * Instances of this class should be used as the Value of derivations, because we don't want to calculate the destination
 * state from the MethodCall until we're sure we need to actually evaluate the derivation (i.e. calculating its
 * compatibility).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class LazyStateValue extends StateValue {

    private final MethodCall methodCall;
    private final Supplier<State> memoizedResultState;

    /**
     * @param initialState This object will not be modified by this class.
     */
    public LazyStateValue(State initialState, MethodCall methodCall) {
        this.methodCall = methodCall;
        this.memoizedResultState = Suppliers.memoize(()-> methodCall.invokeOnDeepCopyOfState(initialState));
    }

    @Override
    public State getState() {
        return memoizedResultState.get();
    }

    public MethodCall getMethodCall() {
        return methodCall;
    }

    /**
     * Based on {@link #getState()}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LazyStateValue that = (LazyStateValue) o;
        return Objects.equals(getState(), that.getState());
    }

    /**
     * Based on {@link #getState()}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getState());
    }
}
