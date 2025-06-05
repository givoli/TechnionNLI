package il.ac.technion.nlp.nli.parser.experiment.analysis;

import ofergivoli.olib.data_structures.map.SafeHashMap;
import com.thoughtworks.xstream.XStream;

import java.io.Serializable;

/**
 * Instances better be serialized using java native serialization rather than using {@link XStream} because the latter
 * would inline the same string many times, resulting in a huge XML file.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class FeatureDataForCandidateLogicalFormsInMultipleExamples implements Serializable {

    public final SafeHashMap<String, SafeHashMap<String, ExampleAndFeatureSpecificData>>
            featureToExampleIdToExampleSpecificData = new SafeHashMap<>();

    public static class ExampleAndFeatureSpecificData {
        public int sumOfFeatureValuesInCorrectLfCandidates;
        public int sumOfFeatureValuesInIncorrectLfCandidates;
    }

    public static class ExampleSpecificData {
        int numberOfCorrectLfCandidates;
        int numberOfIncorrectLfCandidates;
    }

}
