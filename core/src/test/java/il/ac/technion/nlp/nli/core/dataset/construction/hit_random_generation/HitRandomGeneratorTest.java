package il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation;

import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.dataset.simple_test_domain.SimpleSocialNetwork;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class HitRandomGeneratorTest {

    private static class StateVisualizerDummyImpl extends StateVisualizer {

        private static final long serialVersionUID = 2429330874180060509L;

        @Override
        public HtmlString getVisualRepresentation(State state) {
            return null;
        }
    }

    private static class HitRandomGeneratorDummyImpl extends HitRandomGenerator {

        @Nullable
        @Override
        protected NliRootEntity generateRandomRootEntityForInitialState() {
            return null;
        }

        @Nullable
        @Override
        protected MethodCall generateRandomFunctionCall(State initialState, StateVisualizer initialStateVisualizer,
                                                        StateVisualizer desiredStateVisualizer) {
            return null;
        }

        public HitRandomGeneratorDummyImpl() {
            super(new Domain("NA", SimpleSocialNetwork.class),
                    new Random(0), StateVisualizerDummyImpl::new);
        }
    }


    @Test
    public void testSamplingOverUniformAndGeometricLikeDistribution() throws Exception {

        HitRandomGenerator hitGenerator = new HitRandomGeneratorDummyImpl();

        ArrayList<Integer> array = new ArrayList<>(); // array.get(i) == i
        for (int i=0; i<4; i++)
            array.add(i);

        // For both of the following, element with index i count the number of time we sampled array.get(i):
        ArrayList<Integer> uniformCounts = new ArrayList<>(Collections.nCopies(array.size(), 0));
        ArrayList<Integer> geometricCounts = new ArrayList<>(Collections.nCopies(array.size(), 0));

        double p = 0.7;
        for (long i=0; i<100000; i++) {
            int sampledInd = hitGenerator.sampleUniformly(array);
            uniformCounts.set(sampledInd, uniformCounts.get(sampledInd)+1);

            sampledInd = hitGenerator.sampleOverFiniteGeometricDistribution(p, array);
            geometricCounts.set(sampledInd, geometricCounts.get(sampledInd)+1);
        }

        for (int i=0; i<geometricCounts.size(); i++) {
            double ratio = (double) uniformCounts.get(i) / uniformCounts.get(0);
            assertNumbersAreClose(ratio, 1);

            ratio = (double) geometricCounts.get(i) / geometricCounts.get(0);
            assertNumbersAreClose(ratio, Math.pow(1-p,i));
        }
    }



    private void assertNumbersAreClose(double n1, double n2) {
        final double eps = 0.2;
        if (Math.abs(n1) < Math.pow(eps,4)) {
            assertTrue(Math.abs(n1 - n2) < Math.pow(eps, 3)); // expect their difference to be close to 1.
            throw new RuntimeException("currently not expecting this");
        }
        else {
            assertTrue(Math.abs(n2 / n1 - 1) < eps); // expect their quotient to be close to 1
        }
    }
}