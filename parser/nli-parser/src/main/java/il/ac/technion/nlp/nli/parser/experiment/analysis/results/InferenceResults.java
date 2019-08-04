package il.ac.technion.nlp.nli.parser.experiment.analysis.results;

import com.google.common.base.Verify;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class InferenceResults {

    /**
     * The value reported by Sempre.
     */
    public final double correct;

    /**
     * The time the inference took, in seconds.
     */
    public final double time;

    public InferenceResults(double correct, double time) {
        Verify.verify(correct >= -0.0001 && correct <= 1.0001);
        this.correct = correct;
        this.time = time;
    }
}
