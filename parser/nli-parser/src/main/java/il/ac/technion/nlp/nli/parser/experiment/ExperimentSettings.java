package il.ac.technion.nlp.nli.parser.experiment;

import com.google.common.base.Verify;
import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import edu.stanford.nlp.sempre.FloatingParser;
import edu.stanford.nlp.sempre.Params;
import edu.stanford.nlp.sempre.tables.features.PhrasePredicateFeatureComputer;
import il.ac.technion.nlp.nli.core.dataset.Example;
import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;
import il.ac.technion.nlp.nli.parser.experiment.analysis.ExperimentAnalysisSettings;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines the exact settings in which an experiment run.
 * To be sent (serialized) to remote hosts which will run the experiment and send back the results.
 *
 * All the path fields, unless specified otherwise, are either absolute or relative to the resource directory of this
 * module.
 */
public class ExperimentSettings implements Serializable {

    private static final long serialVersionUID = -8508792813064989008L;


    /**
     * Tags contain information about the experiment that isn't used when executing the experiment.
     */
    public final SafeMap<String, String> tagNameToValue = new SafeHashMap<>();

    /**
     * The execution of the experiment should be deterministic (might slightly increase execution time).
     */
    public final boolean deterministic = true;

    /**
     * Should not be null in in-domain and zero-shot setups. May be null in any other setup (e.g. training on all
     * training data without evaluation).
     */
    public @Nullable String testDomainId;

    public SetupType setupType;

    /**
     * Actual train examples to be used.
     * For 2-step training experiments, the split of these examples over the two steps is defined by
     * {@link #firstStepTrainingDomainIds}.
     *
     * Implementation note: We don't want to reference the examples themselves because we want to keep the serialized
     * representation of this object small (so it could be quickly transferred to remote hosts).
     */
    public List<String> trainingExampleIds;
    /**
     * Actual test examples to be used.
     */
    public List<String> testExampleIds;

    /**
     * The number of train-test iteration to perform. The test/dev set is tested after each training iteration (and is
     * reported as part of that iteration's results).
     * For 2-step training experiments, this defines the iterations number of the second step.
     * For evaluation-only setup this should be 1.
     */
    public int iterationsNum;

    public boolean shuffleTrainingExamplesBetweenIterations = false;


    public double regularizationCoefficient = 3e-5;

    /**
     * Corresponds to {@link Params.Options#initStepSize}.
     */
    public double initStepSize = 1;


    /**
     * Corresponds to {@link Params.Options#lazyL1FullUpdateFreq}, except that -1 means using non-lazy
     * regularization.
     */
    public int lazyL1FullUpdateFreq = -1;


    /**
     * Max number of derivations to keep in a cell during inference.
     */
    public int beamSize = 200;


    /**
     * The value for {@link PhrasePredicateFeatureComputer.Options#maxNforLexicalizeAllPairs}.
     */
    public int maxNgramLengthForLexicalizedPhrasePredicateFeatures = 2;

    /**
     * The value for {@link FloatingParser.Options#maxDepth}
     */
    public int maxDerivationSize = 15;


    /**
     * when true, after all training iterations are done we test on the training set without modifying the weifghts (i.e.
     * with the same weights used for testing on the test set in the last iteration).
     */
    public boolean testOnTrainSetAfterAllIterations = false;
    public boolean useAppLogicFiltering = true;
    public boolean useLexicalizedPhrasePredicateFeatures = true;
    public boolean extractUnlexicalizedPhrasePredicateFeaturesWithoutPos = true;
    public boolean usePredicatesThatAreProbablyDomainSpecificForExtractingLexicalizedPhrasePredicateFeatures = true;
    public boolean useDescriptionPhraseFeatures = true;

    /**
     * When this is false, we don't use any features beyond those already extracted by Sempre.
     * null is equivalent to true (for backward compatibility).
     */
    public Boolean enableInstructionFeatures = true;


    /**
     * This value is in the range (0,1], and it's the potion wight updates that gets carried out regardless of
     * existence of evidence suggesting the update is useful for multiple domains.
     * When null we do full unconditional updates (i.e. as if the value if 1.0).
     */
    public @Nullable Double unconditionalWeightUpdateFraction;

    /**
     * CWU = conditional weight update.
     * Should be null only if unconditionalWeightUpdateFraction is null.
     */
    public @Nullable Integer domainsNumberRequiredForCwu;



    public boolean twoStepTraining = false;

    /**
     * If null and it's a 2-step training experiment, then the value of {@link #regularizationCoefficient} is used.
     * Should be null when it's not a 2-step training experiment.
     */
    public @Nullable Double firstStepRegularizationCoefficient = null;


    /**
     * If null and it's a 2-step training experiment, then the value of {@link #initStepSize} is used.
     * Should be null when it's not a 2-step training experiment.
     */
    public @Nullable Double firstStepInitStepSize = null;

    /**
     * null iff the experiment is not a 2-step training experiment.
     */
    public @Nullable Integer firstStepIterationsNum;

    /**
     * All domains not in this list, and excluding the test domain, will be used as training in the second step.
     * null iff the experiment is not a 2-step training experiment.
     */
    public @Nullable List<String> firstStepTrainingDomainIds;

    /**
     * The domain ordering used to define the partition of domains into first and second steps.
     * null iff the experiment is not a 2-step training experiment.
     */
    public @Nullable List<String> domainOrdering;

    /**
     * If not null, the initial weights are read from this file.
     */
    public @Nullable File initialWeightsFileAbsoluteOrRelativeToExperimentDir;

    /**
     * Can be null if not used.
     * Note: this is not {@link Path} because this class needs to be serializable.
     */
    public @Nullable File featuresGeneralityScoresXml;



    /**
     * Must be null in non 2-step experiments.
     * In 2-step experiments, if this is null, then all features get their weights updated in the second step.
     * If not null, the features that get their weights updated are those that have the top generality score,
     * determined by {@link #featuresGeneralityScoresXml}.
     */
    public  @Nullable Integer numOfFeaturesToUpdateWeightsForInStep2;



    /**
     * When not null, only the this many features are allowed to be extracted (those with highest generality score, even
     * if due to other options they are not in practice extracted).
     * This field can hold a value higher than the total number of features in the feature generality scores file.
     */
    public @Nullable Integer featuresAllowedToBeExtractedNum;



    public ExperimentAnalysisSettings analysisSettings = new ExperimentAnalysisSettings();



    /**
     * @param trainTestSplit only used during construction (reference not saved).
     * @param maxTrainingExamplesNum if null then all are used.
     * @param maxTestExamplesNum if null then all are used.
     */
    public ExperimentSettings(ExampleSplit trainTestSplit,
                              @Nullable String testDomainId, SetupType setupType,
                              @Nullable Integer maxTrainingExamplesNum, @Nullable Integer maxTestExamplesNum,
                              int iterationsNum) {

        Verify.verify(iterationsNum>0);

        this.testDomainId = testDomainId;
        this.setupType = setupType;
        this.iterationsNum = iterationsNum;
        int trainSetSize = getActualTrainSetSize(trainTestSplit, maxTrainingExamplesNum);
        this.trainingExampleIds = trainTestSplit.getTrainExamples().subList(0,trainSetSize).stream()
                .map(Example::getId).collect(Collectors.toList());
        int testSetSize = getActualTestSetSize(trainTestSplit, maxTestExamplesNum);
        this.testExampleIds = trainTestSplit.getTestExamples().subList(0,testSetSize).stream()
                .map(Example::getId).collect(Collectors.toList());
    }


    /**
     * @return The number of training examples that are actually used.
     */
    private int getActualTrainSetSize(ExampleSplit trainTestSplit, @Nullable Integer maxTrainExamplesNum) {
        int trainSetSize = trainTestSplit.getTrainExamples().size();
        return maxTrainExamplesNum ==null ? trainSetSize : Math.min(maxTrainExamplesNum, trainSetSize);
    }

    /**
     * @return The number of test examples that are actually used.
     */
    private int getActualTestSetSize(ExampleSplit trainTestSplit, @Nullable Integer maxTestExamplesNum) {
        int testSetSize = trainTestSplit.getTestExamples().size();
        return maxTestExamplesNum ==null ? testSetSize : Math.min(maxTestExamplesNum, testSetSize);
    }



    public enum SetupType { //TODO: move this class outside.
        IN_DOMAIN,
        ZERO_SHOT,
        /**
         * A setup that does not include evaluation (e.g. when you only interested in learning weights).
         */
        TRAINING_ONLY,
        /**
         * A setup that does not include training (the final weights used the the initial ones read from file).
         */
        EVALUATING_ONLY
    }


    /**
     * The {@link #testExampleIds} and {@link #initialWeightsFileAbsoluteOrRelativeToExperimentDir} fields of this
     * object do not affect the returned value (in which these fields are set according the arguments).
     * @param idsOfExamplesToEvaluateOn reference to a shallow copy is kept.
     * @return a clone of this object, set such that only evaluation takes place (no training).
     */
    public ExperimentSettings getEvaluationOnlyVersion(
            List<String> idsOfExamplesToEvaluateOn,
            Path initialWeightsFileForEvaluationAbsoluteOrRelativeToExperimentDir){

        ExperimentSettings result = deepCopy();

        result.setupType = SetupType.EVALUATING_ONLY;
        result.iterationsNum = 1;
        result.initialWeightsFileAbsoluteOrRelativeToExperimentDir =
                initialWeightsFileForEvaluationAbsoluteOrRelativeToExperimentDir.toFile();
        result.testExampleIds = new LinkedList<>(idsOfExamplesToEvaluateOn);

        result.trainingExampleIds = new LinkedList<>();
        result.testOnTrainSetAfterAllIterations = false;

        return result;
    }


    public ExperimentSettings deepCopy() {
        return org.apache.commons.lang3.SerializationUtils.clone(this);
    }



}
