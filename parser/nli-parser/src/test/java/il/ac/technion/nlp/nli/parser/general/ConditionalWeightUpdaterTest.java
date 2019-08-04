package il.ac.technion.nlp.nli.parser.general;

import il.ac.technion.nlp.nli.core.dataset.Domain;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ConditionalWeightUpdaterTest {


    private final double EPSILON = 1e-7;

    private final Domain d1 = createDummyDomain("d1");
    private final Domain d2 = createDummyDomain("d2");
    private final Domain d3 = createDummyDomain("d3");
    private final Domain d4 = createDummyDomain("d4");


    @Test
    public void sanityCheckWhenEntireRequestedUpdatesAreAlwaysAppliedUnconditionally() {

        for (ConditionalWeightUpdater weightUpdate : Arrays.asList(
                new ConditionalWeightUpdater(1.0, 4, false),
                new ConditionalWeightUpdater(0.1, 1, false))) {


            ArrayList<Domain> domainArray = new ArrayList<>(Arrays.asList(d1, d2, d3));
            Random rand = new Random(0);
            for (int i = 0; i < 1000; i++) {
                Domain randomDomain = domainArray.get(rand.nextInt(domainArray.size()));
                String randomFeature = "f" + rand.nextInt(10);
                double randomUpdate = (rand.nextDouble() - 0.5) * 100;
                assertEquals(randomUpdate, weightUpdate.calcWeightUpdate(randomFeature, randomDomain, randomUpdate),
                        EPSILON);
            }
        }
    }



    @Test
    public void testWithSingleDomain() {

        ConditionalWeightUpdater weightUpdate = new ConditionalWeightUpdater(0.1, 2, false);

        addDataOfNonParticipatingFeatures(weightUpdate);

        assertEquals(10, weightUpdate.calcWeightUpdate("f2", d1, 100), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 90, 0, 0, 0);
        assertEquals(5, weightUpdate.calcWeightUpdate("f2", d1, 50), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 135, 0, 0, 0);
        assertEquals(-1, weightUpdate.calcWeightUpdate("f2", d1, -10), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 126, 0, 0, 0);
        assertEquals(-20, weightUpdate.calcWeightUpdate("f2", d1, -200), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", -54, 0, 0, 0);

    }


    @Test
    public void testWithMultipleDomains() {

        ConditionalWeightUpdater weightUpdate = new ConditionalWeightUpdater(0.1, 2, false);

        addDataOfNonParticipatingFeatures(weightUpdate);

        assertEquals(10, weightUpdate.calcWeightUpdate("f2", d1, 100), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 90, 0, 0, 0);
        assertEquals(-9, weightUpdate.calcWeightUpdate("f2", d2, -90), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 90, -81, 0, 0);
        assertEquals(80, weightUpdate.calcWeightUpdate("f2", d3, 80), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 10, -81, 0, 0);
        assertEquals(10, weightUpdate.calcWeightUpdate("f2", d3, 70), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 0, -81, 60, 0);
        assertEquals(5, weightUpdate.calcWeightUpdate("f2", d3, 50), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 0, -81, 105, 0);
        assertEquals(105, weightUpdate.calcWeightUpdate("f2", d1, 200), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 95, -81, 0, 0);
        assertEquals(-5, weightUpdate.calcWeightUpdate("f2", d1, -50), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 50, -76, 0, 0);
        assertEquals(-50, weightUpdate.calcWeightUpdate("f2", d1, -100), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 0, -26, 0, 0);
    }



    /**
     * @param weightUpdate expected to have {@link ConditionalWeightUpdater#unconditionalWeightUpdateFraction} of 0.1
     *                     and {@link ConditionalWeightUpdater#domainsNumberRequired} of 2.
     */
    private void addDataOfNonParticipatingFeatures(ConditionalWeightUpdater weightUpdate) {


        // adding weights to features other than "f2", just to make sure it doesn't effect f2.
        assertEquals(1, weightUpdate.calcWeightUpdate("f1", d1, 10), EPSILON);
        assetPendingWeightValues(weightUpdate, "f1", 9, 0, 0, 0);
        assertEquals(1, weightUpdate.calcWeightUpdate("f3", d2, 10), EPSILON);
        assetPendingWeightValues(weightUpdate, "f3", 0, 9, 0, 0);
        assertEquals(0.5, weightUpdate.calcWeightUpdate("f4", d1, 5), EPSILON);
        assetPendingWeightValues(weightUpdate, "f4", 4.5, 0, 0, 0);
        assertEquals(4.5, weightUpdate.calcWeightUpdate("f4", d2, 20), EPSILON);
        assetPendingWeightValues(weightUpdate, "f4", 0, 15.5, 0, 0);

    }





    @Test
    public void testWithMoreThanTwoRequiredDomains() {

        ConditionalWeightUpdater weightUpdate = new ConditionalWeightUpdater(0.1, 3, false);

        // 3 domains
        assertEquals(10, weightUpdate.calcWeightUpdate("f2", d1, 100), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 90, 0, 0, 0);
        assertEquals(10,  weightUpdate.calcWeightUpdate("f2", d2, 100), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 80, 90, 0, 0);
        assertEquals(80, weightUpdate.calcWeightUpdate("f2", d3, 100), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 0, 10, 20, 0);

        // 4 domains:
        weightUpdate = new ConditionalWeightUpdater(0.1, 3, false);
        assertEquals(10, weightUpdate.calcWeightUpdate("f2", d1, 100), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 90, 0, 0, 0);
        assertEquals(20, weightUpdate.calcWeightUpdate("f2", d2, 200), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 70, 180, 0, 0);
        assertEquals(50, weightUpdate.calcWeightUpdate("f2", d3, 50), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 20, 130, 0, 0);
        assertEquals(-10, weightUpdate.calcWeightUpdate("f2", d4, -100), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 20, 130, 0, -90);
        assertEquals(-10, weightUpdate.calcWeightUpdate("f2", d1, -100), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", -70, 130, 0, -80);
        assertEquals(-70, weightUpdate.calcWeightUpdate("f2", d2, -220), EPSILON);
        assetPendingWeightValues(weightUpdate, "f2", 0, -20, 0, -10);

    }

    private Domain createDummyDomain(String id) {
        return new Domain(id, il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities.LightingControlSystem
                .class);
    }

    /**
     * The parameters u1,...,u4 are the pending
     */
    private void assetPendingWeightValues(ConditionalWeightUpdater weightUpdate, String featureName,
                                          double u1, double u2, double u3, double u4) {

        assertEquals(u1, weightUpdate.getPendingUpdate(featureName, d1), EPSILON);
        assertEquals(u2, weightUpdate.getPendingUpdate(featureName, d2), EPSILON);
        assertEquals(u3, weightUpdate.getPendingUpdate(featureName, d3), EPSILON);
        assertEquals(u4, weightUpdate.getPendingUpdate(featureName, d4), EPSILON);
    }

}