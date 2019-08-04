package il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation;

import java.util.Random;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class FiniteGeometricDistributionIntegerGenerator extends RandomIntegerGenerator {

    private final double p;

    /**
     * The numbers generated will be in the range [0, endOfSamplingRangeExclusive-1].
     * @param p the "success" probability.
     */
    public FiniteGeometricDistributionIntegerGenerator(double p, int endOfSamplingRangeExclusive) {
        super(endOfSamplingRangeExclusive);
        this.p = p;
    }

    @Override
    public int generate(Random rand) {
        double eps = 0.000000001;
        assert (p>0 && p<=1+eps);

        int result = 0;
        while (true) {
            if (rand.nextDouble() < p)
                return result; // "success"
            result++;
            if (result>=endOfSamplingRangeExclusive)
                result = 0; // there was no "success" for any number, so we start from the beginning.
        }

        /**
         * Nice proof that this implementation yields the documented distribution:
         * Let us denote with n the number of "failed iterations". So in iteration n+1 is the "successful iteration" in
         * which one of the numbers is successfully sampled.
         * Given that the j iteration is successful, the proportions between the probabilities of the numbers is as
         * desired (by definition). Let us denote those probabilities with P(i|S_j), where i is the number, and S_j is
         * the event that iteration j was successful.
         * Thus:   P(i) = P(i|S_1)*P(S_1) + P(i|S_2)*P(S_2) + ...
         *              = P(i|S_1)*P(S_1) + P(i|S_1)*P(S_2) + ...
         *              = P(i|S_1) ( P(S_1) + PP(S_2) + ... )
         *              = P(i|S_1)
         * And thus the ratios between the P(i) probabilities are as desired.
         */
    }
}
