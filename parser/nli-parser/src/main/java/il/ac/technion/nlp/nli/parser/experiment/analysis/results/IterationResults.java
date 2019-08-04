package il.ac.technion.nlp.nli.parser.experiment.analysis.results;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import il.ac.technion.nlp.nli.core.dataset.Example;
import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalDouble;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class IterationResults {

    private final ExampleSplit trainTestSplit;
    private SafeMap<Example, InferenceResults> exampleToInferenceResults = new SafeHashMap<>();

    public IterationResults(ExampleSplit trainTestSplit) {
        this.trainTestSplit = trainTestSplit;
    }

    public void addInferenceResults(Example ex, InferenceResults results) {
        Verify.verify(exampleToInferenceResults.put(ex, results) == null);
    }

    /**
     * @return null if no results were added for that example.
     */
    public @Nullable InferenceResults getInferenceResults(Example ex) {
        return exampleToInferenceResults.safeGet(ex);
    }

    /**
     * @return The average correct value of all examples in the specified split part, or null if there are no
     * examples belonging to 'splitPart'.
     */
    public @Nullable Double getAccuracy(ExampleSplit.SplitPart splitPart) {
        //noinspection OptionalGetWithoutIsPresent
        OptionalDouble average = exampleToInferenceResults.keySet().stream()
                .filter(ex -> trainTestSplit.getSplitPart(ex) == splitPart)
                .mapToDouble(ex -> exampleToInferenceResults.safeGet(ex).correct)
                .average();
        return average.isPresent() ? average.getAsDouble() : null;
    }

    /**
     * @return Average inference time (over both train and test sets) in seconds.
     */
    public double getAverageInferenceTime() {
        //noinspection OptionalGetWithoutIsPresent
        OptionalDouble average = exampleToInferenceResults.keySet().stream()
                .mapToDouble(ex -> exampleToInferenceResults.safeGet(ex).time)
                .average();
        if (!average.isPresent())
            throw new RuntimeException("IterationResults is empty");
        return average.getAsDouble();
    }

}
