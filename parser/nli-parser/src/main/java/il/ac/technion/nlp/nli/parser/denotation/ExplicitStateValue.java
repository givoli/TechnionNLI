package il.ac.technion.nlp.nli.parser.denotation;

import il.ac.technion.nlp.nli.core.state.State;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ExplicitStateValue extends StateValue {

    /**
     * See {@link StateValue#getState()}.
     */
    private final @Nullable State state;

    public ExplicitStateValue(@Nullable State state) {
        this.state = state;
    }

    @Override
    public @Nullable State getState() {
        return state;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExplicitStateValue that = (ExplicitStateValue) o;
        return Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state);
    }
}
