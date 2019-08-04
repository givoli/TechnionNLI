package il.ac.technion.nlp.nli.parser;

import com.google.common.base.Verify;
import edu.stanford.nlp.sempre.ListValue;
import edu.stanford.nlp.sempre.Value;
import edu.stanford.nlp.sempre.ValueEvaluator;
import il.ac.technion.nlp.nli.parser.denotation.StateValue;
import il.ac.technion.nlp.nli.core.state.State;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InstructionValueEvaluator implements ValueEvaluator {

    @Override
    public double getCompatibility(Value target, Value pred) {

        List<Value> targetValues = ((ListValue) target).values;
        Verify.verify(targetValues.size() == 1);

        List<Value> predValues = ((ListValue) pred).values;
        Verify.verify(predValues.size() == 1);


        State targetDestState = ((StateValue) targetValues.get(0)).getState();
        State predDestState = ((StateValue) predValues.get(0)).getState();

        return isPredictedStateCorrect(targetDestState, predDestState) ? 1 : 0;
    }

    public static boolean isPredictedStateCorrect(State destinationState, @Nullable State predictedDestinationState) {
        return predictedDestinationState != null && // null means the execution failed.
                predictedDestinationState.entityGraphsEqual(destinationState);

    }
}
