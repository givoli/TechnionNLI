package il.ac.technion.nlp.nli.parser.experiment.analysis.results;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import il.ac.technion.nlp.nli.core.dataset.Example;
import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;
import il.ac.technion.nlp.nli.parser.general.SempreExperiment;
import org.jetbrains.annotations.Nullable;

/**
 * Data about the correctness of predictions made by a {@link SempreExperiment}.
 * Each iteration is referred to by its number (as chosen by the user of this class).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class SempreExperimentResults {

    private final ExampleSplit trainTestSplit;
    /**
     * The largest iteration number (1-based) of results added. Null iff no inference results were added yet.
     */
    private @Nullable Integer lastIteration;
    /**
     * keys - iteration number (1-based).
     */
    private SafeMap<Integer, IterationResults> iterationToIterationResults = new SafeHashMap<>();

    public SempreExperimentResults(ExampleSplit trainTestSplit) {
        this.trainTestSplit = trainTestSplit;
    }

    /**
     * @param iteration the number of the iteration (1-based).
     */
    public void addInferenceResults(int iteration, Example ex, InferenceResults results) {
        if (lastIteration == null)
            lastIteration = iteration;
        Verify.verify(iteration >= lastIteration);
        lastIteration = iteration;

        if (!iterationToIterationResults.safeContainsKey(iteration))
            iterationToIterationResults.put(iteration, new IterationResults(trainTestSplit));

        iterationToIterationResults.safeGet(iteration).addInferenceResults(ex, results);
    }

    public IterationResults getIterationResults(int iteration) {
        return iterationToIterationResults.safeGet(iteration);
    }

    public IterationResults getLastIterationResults() {
        if (lastIteration == null)
            throw new RuntimeException("SempreExperimentResults is empty");
        return getIterationResults(lastIteration);
    }

    /**
     * @return null if there was no such iteration, or if there were no examples for that 'splitPart'.
     */
    public @Nullable Double getAccuracy(ExampleSplit.SplitPart splitPart, int iteration) {
        if (!iterationToIterationResults.safeContainsKey(iteration))
            return null;
        return iterationToIterationResults.safeGet(iteration).getAccuracy(splitPart);
    }


}
