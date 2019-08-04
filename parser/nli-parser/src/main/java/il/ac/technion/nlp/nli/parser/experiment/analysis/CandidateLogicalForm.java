package il.ac.technion.nlp.nli.parser.experiment.analysis;

import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.string.StringManager;
import edu.stanford.nlp.sempre.Params;
import il.ac.technion.nlp.nli.parser.NliMethodCallFormula;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class CandidateLogicalForm implements Serializable{

    private static final long serialVersionUID = 8901836796622146470L;

    public final boolean correctDenotation;
    /**
     * The sum of [feature-value * weight] over all features.
     */
    public final double score;
    public final double scoreOfTopLogicalForms;
    public final NliMethodCallFormula logicalForm;

    /**
     * Key: a feature extracted for this candidate logical form.
     * Value: left - feature value; right - [feature value] * [feature weight].
     * May be null in case the user prefers not to include this data.
     */
    public final @Nullable SafeMap<String,Pair<Double,Double>> featureToValuesAndScoreGain;


    /**
     * @param weights reference to this is not saved. Used only if 'featureValues' is not null.
     * @param featureValues the features extracted for the logical form. May be null if the user does not wish to
     *                      initialize the {@link #featureToValuesAndScoreGain} field.
     */
    public CandidateLogicalForm(NliMethodCallFormula logicalForm, boolean correctDenotation, double score,
                                double scoreOfTopLogicalForms, Params weights,
                                @Nullable Collection<FeatureAndValue> featureValues) {

        this.logicalForm = logicalForm;
        this.correctDenotation = correctDenotation;
        this.score = score;
        this.scoreOfTopLogicalForms = scoreOfTopLogicalForms;

        if (featureValues == null)
            this.featureToValuesAndScoreGain = null;
        else {
            featureToValuesAndScoreGain = new SafeHashMap<>();
            //noinspection ConstantConditions
            featureValues.forEach(feature -> {
                double scoreGain = feature.value * weights.getWeightWithoutUpdate(feature.featureName);
                featureToValuesAndScoreGain.put(feature.featureName, new ImmutablePair<>(feature.value,
                        scoreGain));
            });
        }
    }

    /**
     * Based only on {@link #logicalForm}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandidateLogicalForm)) return false;
        CandidateLogicalForm that = (CandidateLogicalForm) o;
        return Objects.equals(logicalForm, that.logicalForm);
    }

    /**
     * Based only on {@link #logicalForm}
     */
    @Override
    public int hashCode() {
        return Objects.hash(logicalForm);
    }


    @Override
    public String toString() {
        String result = "correctDenotation=" + correctDenotation + "\nscore=" + score + "\nscoreOfTopLogicalForms=" +
                scoreOfTopLogicalForms + "\n\n" + GeneralAnalysisUtils.getHumanFriendlyRepresentationOfFormula(logicalForm) + "\n\n" +
                logicalForm;
        if (featureToValuesAndScoreGain != null) {
            //noinspection RedundantStreamOptionalCall
            result += "\n\nfeature   values  scoreGain\n" +
                    StringManager.collectionToStringWithNewlines(
                            featureToValuesAndScoreGain.entrySet().stream()
                            .sorted(Comparator.comparing(Map.Entry::getKey)) // for determinism.
                            // sort by descending score gain:
                            .sorted(Comparator.comparing(entry->-Math.abs(entry.getValue().getRight())))
                            .map(entry->entry.getKey() + "\t" + entry.getValue().getLeft() + "\t"
                                    + entry.getValue().getRight())
                            .collect(Collectors.toList()));
        }
        return result;
    }

}
