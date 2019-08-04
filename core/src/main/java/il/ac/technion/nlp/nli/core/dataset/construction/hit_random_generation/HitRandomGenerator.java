package il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.io.log.Log;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.NliMethod;
import il.ac.technion.nlp.nli.core.dataset.construction.Hit;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.reflection.EntityGraphReflection;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;



/**
 * Abstract class for randomly generating HITs: all belonging to the same batch (with a label of that batch appearing
 * in the ids of all generated HITs) and to the same domain.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public abstract class HitRandomGenerator {


    protected final Domain domain;

    /**
     * The NLI method for which HITs will be created.
     * TODO: make the older extending classes use this as well.
     */
    protected final NliMethod nliMethod;

    /**
     * See doc of {@link #HitRandomGenerator(Domain, Random, Supplier)}
     */
	protected final Random rand;

    private final Supplier<StateVisualizer> stateVisualizerFactory;


    /**
     * @param domain The domain over which HITs are generated.
     * @param rand Used as the single source of randomness for this class and extending classes.
     * @param stateVisualizerFactory Used to create {@link StateVisualizer} for both the original and desired states.
     */
    public HitRandomGenerator(Domain domain, Random rand,
                              Supplier<StateVisualizer> stateVisualizerFactory, NliMethod nliMethod) {
		this.rand = rand;
        this.stateVisualizerFactory = stateVisualizerFactory;
        this.domain = domain;
        this.nliMethod = nliMethod;
    }

    public HitRandomGenerator(Domain domain, Random rand,
                              Supplier<StateVisualizer> stateVisualizerFactory) {
        this(domain, rand, stateVisualizerFactory, null);
        //TODO: remove this construction after all older code will be using the 'nliMethod' field.
    }

    /**
     * The id of each new HIT returned is generated via {@link java.util.UUID}.
     * The creation time of each new HIT is set to ZonedDateTime.now().
     * @param labelToAddToId Will appear in the id of the generated HIT after the domain id.
     */
    public Hit generateHit(String labelToAddToId) {

        final int MAX_NUM_OF_TRIES_GENERATING_FUNCTION_CALL_PER_INITIAL_STATE = 1000;

        Log.trace("Generating HIT...");

        while (true) { // keep trying until you don't randomly fail.

            State initialState = null;
            while (initialState == null) {

                // We assume this call must succeed at some point.
                initialState = new State(domain, generateRandomRootEntityForInitialState(), true);
            }

            MethodCall fc = null;
            int tries = 0;
            while (fc == null && tries < MAX_NUM_OF_TRIES_GENERATING_FUNCTION_CALL_PER_INITIAL_STATE) {
                tries++;
                StateVisualizer initialStateVisualizer = stateVisualizerFactory.get();
                StateVisualizer desiredStateVisualizer = stateVisualizerFactory.get();
                fc = generateRandomFunctionCall(initialState, initialStateVisualizer, desiredStateVisualizer);
                if (fc == null)
                    continue;
                State destinationState = fc.invokeOnDeepCopyOfState(initialState);
                if(destinationState == null) {
                    Log.warn("fc.invokeOnDeepCopyOfState(initialState) returned null during HIT construction");
                    continue;
                }
                if (destinationState.entityGraphsEqual(initialState)) {
                    Log.trace("Randomly generated a destination state with entity graph identical to the initial state's. Root entity:\n" +
                            EntityGraphReflection.entityGraphToHumanFriendlyString(initialState.getRootEntity(), true) +
                            "\nSo not using these. Trying a new random initial state.");
                    continue;
                }

                // success :)
                String idPrefix = domain.getId() + "__" + labelToAddToId;
                return new Hit(domain, initialState, destinationState, fc, initialStateVisualizer,
                        desiredStateVisualizer, idPrefix, ZonedDateTime.now());

            }
            Log.trace("Failed to randomly generate a MethodCall for state with root entity:\n" +
                    EntityGraphReflection.entityGraphToHumanFriendlyString(initialState.getRootEntity(),true) +
                    "\nTrying a new random initial state.");
        }
    }


    /**
     * Tries again and again until successful.
     * @param pred The predicate. Yields result given the {@link HitConstructionInfo} of the hit.
     */
    public Hit generateHitObeyingPredicate(String labelToAddToId, Predicate<HitConstructionInfo> pred){
        while(true) {
            Hit hit = generateHit(labelToAddToId);
            if (pred.test(hit.getHitConstructionInfo()))
                return hit;
        }
    }



    /**
     * This method is called once per HIT generation (at the beginning of the HIT generation process).
     * @return The newly generated initial state on success, and null if this method randomly failed.
     */
     protected abstract @Nullable
     NliRootEntity generateRandomRootEntityForInitialState();


    /**
     * The method is allowed to randomly fail, possibly after already emphasizing things in initialStateVisualizer and
     * desiredStateVisualizer. The method is also allowed to randomly generate a destination state which is identical to
     * the initial state (which is treated like a random failure).
     *
     * @param initialStateVisualizer for output only: emphasize things as relevant.
     * @param desiredStateVisualizer for output only: emphasize things as relevant.
     * @return The newly generated {@link MethodCall} on success, and null if this method randomly failed.
     *          Note: it's possible this method will always fail (for certain initial states).
     */
     protected abstract @Nullable
     MethodCall generateRandomFunctionCall(
            State initialState, StateVisualizer initialStateVisualizer, StateVisualizer desiredStateVisualizer);


    /**
     * @return a number in the range [from, toExclusive-1], sampled uniformly.
     */
     protected int sampleUniformly(@SuppressWarnings("SameParameterValue") int from, int toExclusive) {
         Verify.verify(toExclusive>from);
         return from + new UniformDistributionIntegerGenerator(toExclusive-from).generate(rand);
         // obviously an overkill here, but for the sake of uniformity...
     }


    /**
     * @param p the success probability defining the finite-geometric distribution.
     * @return a number in the range [from, toExclusive-1], sampled with a finite variant of the geometric distribution:
     * the ratios between probabilities of different numbers is the same as in the geometric distribution, but the
     * probabilities are normalized so their sum is 1.
     * (The smallest number has the highest probability)
     */
    protected int sampleOverFiniteGeometricDistribution(double p, int from, int toExclusive) {
        return from + new FiniteGeometricDistributionIntegerGenerator(p, toExclusive-from)
                .generate(rand);
    }

    /**
     * See: {@link #sampleOverFiniteGeometricDistribution(double, int, int)}
     */
    protected <T> T sampleOverFiniteGeometricDistribution(double p, ArrayList<T> array) {
        return array.get(sampleOverFiniteGeometricDistribution(p,0,array.size()));
    }


    protected <T> T sampleUniformly(ArrayList<T> array) {
        return array.get(sampleUniformly(0,array.size()));
    }



    /**
     * @param c is not modified by this method.
     * @return a list with a random order.
     */
    protected  <T> ArrayList<T> sampleSubsetUniformly(Collection<T> c, int outputListSize) {
        assert (outputListSize <= c.size());
        ArrayList<T> array = new ArrayList<>(c);
        Collections.shuffle(array,rand);
        // We need the create a new list because the returned value from subList() is not serializable.
        return new ArrayList<>(array.subList(0,outputListSize));
    }


    @SafeVarargs
    protected static <T> ArrayList<T> createArrayList(T... elements) {
        return new ArrayList<>(Arrays.asList(elements));
    }


    protected <T extends Enum> T sampleUniformly(Class<T> enumClass) {
        T[] constants = enumClass.getEnumConstants();
        return constants[sampleUniformly(0,constants.length)];
    }


    /**
     * @param p the probability to return true.
     */
    protected boolean sampleOverBernoulliDistribution(double p) {
        return rand.nextDouble() < p;
    }


}
