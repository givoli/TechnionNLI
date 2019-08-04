package il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation;

import java.util.Random;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class UniformDistributionIntegerGenerator extends RandomIntegerGenerator {

    /**
     * The numbers generated will be in the range [0, endOfSamplingRangeExclusive-1].
     */
    public UniformDistributionIntegerGenerator(int endOfSamplingRangeExclusive) {
        super(endOfSamplingRangeExclusive);
    }

    @Override
    public int generate(Random rand) {
        return rand.nextInt(endOfSamplingRangeExclusive);
    }

}
