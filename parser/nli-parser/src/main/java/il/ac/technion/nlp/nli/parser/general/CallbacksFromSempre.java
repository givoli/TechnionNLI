package il.ac.technion.nlp.nli.parser.general;

import com.google.common.base.Verify;
import edu.stanford.nlp.sempre.*;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.experiment.analysis.FeatureAndValue;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;


/**
 * TODO: delete whatever is not required, and move whatever relevant to other classes.
 * Callbacks from the original Sempre code (that doesn't belong in other classes).
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
@SuppressWarnings("UnusedParameters")
public class CallbacksFromSempre {

    /**
     * Reminder: sempre may invoke two consecutive inferences on the same example in order to compute gradients.
     */
    public static void reportSempreStartingInference(Example example) {
        if (ExperimentRunner.isExperimentCurrentlyRunning())
            ExperimentRunner.getCurrentExperiment().startNewInference(example);
    }

    /**
     * Invoked immediately after finishing with an inference (before starting a new one).
     */
    public static void reportSempreFinishedInference() {
        if (ExperimentRunner.isExperimentCurrentlyRunning())
            ExperimentRunner.getCurrentExperiment().endInference();
    }

    /**
     * Called when sempre determines the correctness of a derivation
     */
    @SuppressWarnings("UnusedParameters")
    public static void reportDerivCorrectnessWasSet(Derivation deriv, boolean correct) {
        // currently not used.
    }




    /**
     * Called when Sempre starts a new iteration.
     * @param iterationNum 0-based.
     */
    public static void reportNewIterationBegins(int iterationNum) {
        if (ExperimentRunner.isExperimentCurrentlyRunning())
            ExperimentRunner.getCurrentExperiment().setCurrentIterationNumber(iterationNum+1);
    }


    /**
     * Called for example when beginning a new examples group (in each iteration).
     * @param groupLabel either "train", "dev", or "test".
     */
    public static void reportNewDatasetGroupBegins(String groupLabel) {
        if (ExperimentRunner.isExperimentCurrentlyRunning())
            ExperimentRunner.getCurrentExperiment().newDatasetGroupBegins(groupLabel);
    }

    /**
     * @see #reportNewDatasetGroupBegins(String)
     */
    public static void reportDatasetGroupEnds(String groupLabel) {
        if (ExperimentRunner.isExperimentCurrentlyRunning())
            ExperimentRunner.getCurrentExperiment().analysis.reportDatasetGroupEnds(
                    DatasetSempreGroupLabel.getFromSempreTag(groupLabel));
    }


    /**
     * This method should be called by Sempre whenever an inference finishes.
     * @param correct in the range [0,1]
     */
    public static void reportInferenceResults(Example example, long timeInMs, double correct) {
        if (ExperimentRunner.isExperimentCurrentlyRunning())
            ExperimentRunner.getCurrentExperiment().analysis.processInferenceResults(example, timeInMs, correct);
    }

    /**
     * Note:  anchored cells become floating at level 0 (which is not shown by calling this method).
     * @param pruned whether the derivation is pruned by Sempre.
     */
    public static void reportNewDerivationCreatedByRule(Rule rule, String cellName, Derivation deriv, boolean pruned) {
        if (!ExperimentRunner.isExperimentCurrentlyRunning())
            return;
        ExperimentRunner.getCurrentExperiment().analysis.reportNewDerivationCreatedByRule(rule, cellName, deriv, pruned);
    }

    /**
     * I think all the derivations reported here derive a {@link edu.stanford.nlp.sempre.Formula} that contains
     * a {@link edu.stanford.nlp.sempre.StringValue}.
     */
    public static void reportNewBasicDerivationCreatedFromTokenOrPhrase(
            String anchoredCellName, String floatingCellName, Derivation deriv) {
    }

    public static void reportAbortingInference() {
        ExperimentRunner.getCurrentExperiment().analysis.reportAbortingCurrentInference();
    }

    /**
     * @return true iff sempre should abort the current inference. In that case, only the derivations created so
     * far are considered as candidates.
     */
    public static boolean shouldTheCurrentInferenceBeAborted() {
        return false; // currently unused.
    }


    /**
     * @param gradient a map from feature f to the partial derivation of the objective with respect to the weight of f.
     */
    public static void reportWeightsUpdateBegins(Params weights, Map<String, Double> gradient) {

        if (!ExperimentRunner.isExperimentCurrentlyRunning())
            return;

        ExperimentRunner.getCurrentExperiment().analysis.reportWeightsUpdateBegins(gradient);
    }

    /**
     * Note: Throughout the execution of Sempre, the only stage in which the weights changed (after their
     * initialization) is between calls to {@link #reportWeightsUpdateBegins(Params, Map)} and this method.
     */
    public static void reportWeightsUpdateEnds(Params weights) {
        if (!ExperimentRunner.isExperimentCurrentlyRunning())
            return;

        ExperimentRunner.getCurrentExperiment().analysis.reportWeightsUpdateEnds(weights);
    }

    /**
     * This method is called after finishing extracting all features of all derivation of a given inference.
     * @param derivations The candidate derivations predicted during inference. Ordered by descending score.
     * @param compatibilities matches order of 'derivations'.
     * @param topDerivationsNumber number of derivations receiving the highest score.
     */
    public static void reportDerivationsPredictedByInference(
            Example example, List<Derivation> derivations, double[] compatibilities, int topDerivationsNumber,
            ParserState sempreParserState) {

        if (!ExperimentRunner.isExperimentCurrentlyRunning())
            return;

        Verify.verify(ExperimentRunner.getCurrentExperiment().getCurrentExampleBeingParsed().getId().equals(
                example.getId()));

        ArrayList<Boolean> derivationsCorrectness = Arrays.stream(compatibilities).mapToObj(d->{
            if (d==0)
                return false;
            if (d==1)
                return true;
            throw new RuntimeException("In example " + example.id +
                    " a derivation received a non-binary correctness value (unexpected)");
        }).collect(Collectors.toCollection(ArrayList::new));

        ArrayList<Double> derivationsScores = derivations.stream()
                .map(Derivation::getScore)
                .collect(Collectors.toCollection(ArrayList::new));

        Supplier<ArrayList<List<FeatureAndValue>>> derivationFeatureValues = () -> {
            ArrayList<List<FeatureAndValue>> result = new ArrayList<>();
            derivations.forEach(d->{
                List<FeatureAndValue> features = new LinkedList<>();
                d.getAllFeatureVector().forEach((feature,value)->
                        features.add(new FeatureAndValue(feature, value)));
                result.add(features);
            });
            return result;
        };

        ExperimentRunner.getCurrentExperiment().analysis.reportDerivationsPredictedByInference(
                example, new ArrayList<>(derivations), derivationsCorrectness, derivationFeatureValues,
                derivationsScores, topDerivationsNumber, sempreParserState);

    }


}
