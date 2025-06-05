package il.ac.technion.nlp.nli.parser.experiment.analysis;

import com.google.common.base.Verify;
import com.google.common.primitives.Doubles;
import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import ofergivoli.olib.io.TextIO;
import ofergivoli.olib.io.csv.CsvContent;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Represents an feature and its value, extracted from a derivation.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class FeatureAndValue {
    public final String featureName;
    public final double value;

    public FeatureAndValue(String featureName, double value) {
        this.featureName = featureName;
        this.value = value;
    }

    public static List<FeatureAndValue> readFromSempreWeightFile(Path weightFile) {
        List<String> lines = TextIO.readAllLinesFromFileDetectingEncodingAutomatically(weightFile);
        List<FeatureAndValue> result = new ArrayList<>();
        lines.forEach(l-> {
                String[] parts = l.split("\t");
                if (parts.length != 2)
                    throw new RuntimeException("bad file format");
                result.add(new FeatureAndValue(parts[0], Double.parseDouble(parts[1])));
        });
        return result;
    }

    public static SafeMap<String,Double> readMapFromSempreWeightFile(Path weightFile) {
        SafeMap<String,Double> result = new SafeHashMap<>();
        readFromSempreWeightFile(weightFile).forEach(featureAndValue->
                result.putNewKey(featureAndValue.featureName, featureAndValue.value));
        return result;
    }

        /**
         * For each feature f appearing in either c1 or c2, the value delta is its value in c2 minus its value in c1 (the
         * value is 0 if not appearing).
         * @return a map from feature name to its value delta.
         */
    private static SafeMap<String, Double> getValueDeltasBetweenFeatureCollections(Collection<FeatureAndValue> c1,
                                                                                   Collection<FeatureAndValue> c2) {

        SafeMap<String, Double> featureNameToValueInC1 = getFeatureToWeightMap(c1);
        SafeMap<String, Double> featureNameToValueInC2 = getFeatureToWeightMap(c2);

        addMissingFeaturesWithValueZero(featureNameToValueInC1, featureNameToValueInC2.keySet());
        addMissingFeaturesWithValueZero(featureNameToValueInC2, featureNameToValueInC1.keySet());

        SafeMap<String, Double> result = new SafeHashMap<>();
        featureNameToValueInC1.keySet().forEach(feature ->  {
            Double valueInC1 = featureNameToValueInC1.getExisting(feature);
            Double valueInC2 = featureNameToValueInC2.getExisting(feature);
            Verify.verify(result.put(feature, valueInC2-valueInC1) == null);
        });
        return result;
    }


    /**
     * @return A map from feature name to the value percentile rank, rescaled onto the range [0,1], if there's a single
     * feature with minimal value it will be mapped to 0, and if there's a single feature with maximal value it
     * will be mapped to 1.
     * Tied values get a percentile rank which is the mean of the relevant ranks.
     */
    static SafeMap<String, Double> getFeatureToPercentileRankMapping(SafeMap<String,Double> featureToValue) {

        Verify.verify(!featureToValue.isEmpty());


        ArrayList<FeatureAndValue> featureValueArray = featureToValue.entrySet().stream()
                .map(e-> new FeatureAndValue(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));


        NaturalRanking naturalRanking = new NaturalRanking(TiesStrategy.AVERAGE);
        double[] weights = Doubles.toArray(featureValueArray.stream()
                .map(x->x.value)
                .collect(Collectors.toList()));

        double[] ranks = naturalRanking.rank(weights);

        SafeMap<String, Double> result = new SafeHashMap<>();
        for(int i=0; i<ranks.length; i++){
            double percentileRank = (ranks[i] - 1)  / (ranks.length-1);
            result.putNewKey(featureValueArray.get(i).featureName, percentileRank);
        }

        return result;
    }




    /**
     * Returns a string representing the delta between the two param files.
     */
    public static void compareTwoSempreWeightFiles(Path weightFile1, Path weightFile2, Path outputCsv,
                                                   Path outputMoreInfo) {

        List<FeatureAndValue> featureAndWeights1 = FeatureAndValue.readFromSempreWeightFile(weightFile1);
        List<FeatureAndValue> featureAndWeights2 = FeatureAndValue.readFromSempreWeightFile(weightFile2);

        SafeMap<String,Double> featureToWeight1 = getFeatureToWeightMap(featureAndWeights1);
        SafeMap<String,Double> featureToWeight2 = getFeatureToWeightMap(featureAndWeights2);

        SafeMap<String, Double> featureNameToWeightDelta = getValueDeltasBetweenFeatureCollections(
                featureAndWeights1, featureAndWeights2);


        SafeMap<String,Double> featureToWeight1_withMissingValuesAsZeros = new SafeHashMap<>(featureToWeight1);
        addMissingFeaturesWithValueZero(featureToWeight1_withMissingValuesAsZeros, featureToWeight2.keySet());

        SafeMap<String,Double> featureToWeight2_withMissingValuesAsZeros = new SafeHashMap<>(featureToWeight2);
        addMissingFeaturesWithValueZero(featureToWeight2_withMissingValuesAsZeros, featureToWeight1.keySet());

        Verify.verify(featureToWeight1_withMissingValuesAsZeros.keySet().equals(
                featureToWeight2_withMissingValuesAsZeros.keySet()));

        SafeMap<String, Double> featureToPercentileRank1 =
                getFeatureToPercentileRankMapping(featureToWeight1_withMissingValuesAsZeros);

        SafeMap<String, Double> featureToPercentileRank2 =
                getFeatureToPercentileRankMapping(featureToWeight2_withMissingValuesAsZeros);


        double percentileRankOfZero1 = getPercentileRankOfAddedZeroDummyFeature(
                featureToWeight1_withMissingValuesAsZeros);

        double percentileRankOfZero2 = getPercentileRankOfAddedZeroDummyFeature(
                featureToWeight2_withMissingValuesAsZeros);


        CsvContent csv = new CsvContent("feature","weight1","weight2", "weightDiff",
                "percentileRank1", "percentileRank2", "percentileRankDiff",
                "relativePercentileRank1", "relativePercentileRank2", "relativePercentileRankDiff");


        featureToWeight1_withMissingValuesAsZeros.keySet().forEach(feature->{
                    double percentileRank2 = featureToPercentileRank2.safeGet(feature);
                    double percentileRank1 = featureToPercentileRank1.safeGet(feature);
                    double percentileRankDiff = percentileRank2 - percentileRank1;
                    double relativePercentileRank1 = percentileRank1 - percentileRankOfZero1;
                    double relativePercentileRank2 = percentileRank2 - percentileRankOfZero2;
                    double relativePercentileRankDiff = relativePercentileRank2 - relativePercentileRank1;
                    csv.convertObjectsToStringsAndAddRow(
                            feature, featureToWeight1.safeGet(feature), featureToWeight2.safeGet(feature),
                            featureNameToWeightDelta.getExisting(feature),
                            percentileRank1, percentileRank2, percentileRankDiff,
                            relativePercentileRank1, relativePercentileRank2, relativePercentileRankDiff);
                }
        );

        csv.getSortedShallowCopy(9, Double::parseDouble).writeEntireCsv(outputCsv);


        String moreInfo = "percentileRankOfZero1\t" + percentileRankOfZero1 + "\n" +
                "percentileRankOfZero2\t" + percentileRankOfZero2 + "\n";
        TextIO.writeTextToFileInStandardEncoding(outputMoreInfo, moreInfo, true);
    }

    /**
     * @return the returned value is remapped to the range [0,1].
     */
    private static double getPercentileRankOfAddedZeroDummyFeature(SafeMap<String, Double> featureToValue) {
        SafeMap<String, Double> copy = new SafeHashMap<>(featureToValue);
        String dummyFeature = "dummy_feature__" + FeatureAndValue.class.getCanonicalName();
        copy.putNewKey(dummyFeature, 0.0);
        return getFeatureToPercentileRankMapping(copy).getExisting(dummyFeature);
    }

    private static SafeMap<String, Double> getFeatureToWeightMap(Collection<FeatureAndValue> featureAndWeights) {
        SafeMap<String, Double> result = new SafeHashMap<>();
        featureAndWeights.forEach(featureAndValue->
                result.putNewKey(featureAndValue.featureName,featureAndValue.value));
        return result;
    }

    /**
     * Adds entities to 'featureToValue'.
     */
    private static void addMissingFeaturesWithValueZero(SafeMap<String, Double> featureToValue,
                                                        Collection<String> featuresToAddIfMissing){
        featuresToAddIfMissing.forEach(feature->{
            if (!featureToValue.safeContainsKey(feature))
                featureToValue.putNewKey(feature, 0.0);
        });
    }

}
