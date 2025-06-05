package il.ac.technion.nlp.nli.parser.experiment.analysis;

import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class FeatureAndValueTest {

    @Test
    public void testGetFeatureToPercentileRankMapping() {

        SafeMap<String,Double> featuresToValue = new SafeHashMap<>();

        featuresToValue.putNewKey("a",0.0);
        featuresToValue.putNewKey("b",0.1);
        featuresToValue.putNewKey("c",-0.1);
        featuresToValue.putNewKey("d",0.0);


        SafeMap<String, Double> featureToPercentileRank =
                FeatureAndValue.getFeatureToPercentileRankMapping(featuresToValue);

        double epsilon = 1e-5;

        assertEquals(0.5,featureToPercentileRank.getExisting("a"), epsilon);
        assertEquals(1,featureToPercentileRank.getExisting("b"), epsilon);
        assertEquals(0,featureToPercentileRank.getExisting("c"), epsilon);
        assertEquals(0.5,featureToPercentileRank.getExisting("d"), epsilon);

    }

}