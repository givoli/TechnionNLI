package il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation;

import java.util.Random;

/**
 * Random integer generator.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public abstract class RandomIntegerGenerator {

    /**
     * see constructor.
     */
    protected final int endOfSamplingRangeExclusive;

    /**
     * The numbers generated will be in the range [0, endOfSamplingRangeExclusive-1].
     */
    public RandomIntegerGenerator(int endOfSamplingRangeExclusive) {
        assert (endOfSamplingRangeExclusive>0);
        this.endOfSamplingRangeExclusive = endOfSamplingRangeExclusive;
    }

    /**
     * The numbers generated will be in the range [0, endOfSamplingRangeExclusive-1].
     * @param rand source of randomness.
     */
    public abstract int generate(Random rand);
}
