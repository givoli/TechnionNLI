package il.ac.technion.nlp.nli.parser.general;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import edu.stanford.nlp.sempre.Parser;
import il.ac.technion.nlp.nli.core.dataset.Example;
import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentSettings;
import il.ac.technion.nlp.nli.parser.experiment.analysis.FeatureGeneralityTools;
import il.ac.technion.nlp.nli.parser.experiment.analysis.SempreExperimentAnalysis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents an entire Sempre experiment (i.e. Sempre learning process)(e.g. 4 iterations of train & dev).
 * Note that when running a 2-step training experiment, each step is represented by its own {@link SempreExperiment} object.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class SempreExperiment {

    public final Path moduleResourceDir;

    /**
     * This is null if the {@link Parser} was not yet created.
     */
    @Nullable public Parser sempreParser;

    /**
     * In case {@link edu.stanford.nlp.sempre.Dataset#opts#maxExamples} is not empty, this split may contain examples
     * that won't be used.
     */
    public final ExampleSplit trainTestSplit;
    public final SempreExperimentAnalysis analysis;

    /**
     * true if this object represents a first-step in a 2-step execution.
     */
    public final boolean isFirstStep;

    /**
     * When not null, only the weights of these features are considered by the optimization algorithm as variables to
     * optimize (all other weights are considered as constants by the optimization algorithm).
     * A null value represents all possible features.
     */
    public final @Nullable SafeSet<String> featuresToOptimizeWeightsFor;

    /**
     * When not null, only these features are allowed to be extracted (i.e. these are the features that are used
     * by the model).
     * A null value means not adding restrictions.
     */
    public @Nullable SafeSet<String> featuresAllowedToBeExtracted;

    /**
     * null iff not used.
     */
    public final @Nullable ConditionalWeightUpdater conditionalWeightUpdater;


    /**
     * See getter. Set and updated by external logic.
     */
    private DatasetSempreGroupLabel currentDatasetGroupLabel;
    /**
     * See getter. Updated by external logic.
     */
    private @Nullable Integer currentIterationNumber;

    /**
     * null iff no inference started yet.
     */
    private @Nullable InferenceData currentInferenceData;

    /**
     * The reason that this is not a field in {@link InferenceData} is that it needs to be used by the constructor of
     * {@link InferenceData}.
     * It is cleared by {@link #endInference()}.
     */
    public FeatureNameStorage featureNameStorageOfCurrentInference = new FeatureNameStorage();


    /**
     * Contains both the train and test examples.
     */
    public final SafeMap<String, Example> exampleIdToExample;

    public final ExperimentSettings settings;



    /**
     * @param outputAnalysisDir may already exist.
     * @param featuresToOptimizeWeightsFor if null, all weights are being optimized.
     */
    public SempreExperiment(Path moduleResourceDir, ExampleSplit split, File outputAnalysisDir,
                            ExperimentSettings settings, boolean isFirstStep,
                            @Nullable SafeSet<String> featuresToOptimizeWeightsFor) {

        this.isFirstStep = isFirstStep;

        exampleIdToExample = new SafeHashMap<>();
        split.getAllExamples().forEach(ex-> exampleIdToExample.putNewKey(ex.getId(), ex));

        this.moduleResourceDir = moduleResourceDir;
        this.settings = settings;
        this.featuresToOptimizeWeightsFor = featuresToOptimizeWeightsFor;
        if (settings.featuresAllowedToBeExtractedNum != null) {
            //noinspection ConstantConditions
            this.featuresAllowedToBeExtracted = FeatureGeneralityTools.getFeaturesByGeneralityScoreRank(
                    settings.featuresAllowedToBeExtractedNum,
                    moduleResourceDir.resolve(settings.featuresGeneralityScoresXml.toPath()));
        }
        this.trainTestSplit = split;
        this.conditionalWeightUpdater = settings.unconditionalWeightUpdateFraction ==null ? null :
                new ConditionalWeightUpdater(settings.unconditionalWeightUpdateFraction,
                        Objects.requireNonNull(settings.domainsNumberRequiredForCwu),
                        settings.analysisSettings.collectAggregatedAnalysisDataForCWU);


        analysis = new SempreExperimentAnalysis(this, split, settings.analysisSettings, outputAnalysisDir);

    }


    public DatasetSempreGroupLabel getCurrentDatasetGroupLabel() {
        return currentDatasetGroupLabel;
    }

    /**
     * @return The current iteration number. Starts at 1 (unlike in Sempre where the iteration # starts at 0).
     *         Returns null in case the first iterations didn't start yet.
     */
    public Integer getCurrentIterationNumber() {
        return currentIterationNumber;
    }


    public @NotNull  Example getCurrentExampleBeingParsed(){
        assert currentInferenceData != null;
        Example result = currentInferenceData.example;
        Verify.verify(result != null);
        return result;
    }

    public @NotNull InferenceData getCurrentInferenceData(){
        InferenceData result = this.currentInferenceData;
        Verify.verify(result != null);
        return result;
    }


    /**
     *
     * @param currentIterationNumber 1-based (unlike in Sempre where the iteration # starts at 0).
     */
    public void setCurrentIterationNumber(int currentIterationNumber) {
        Verify.verify(currentIterationNumber >= 1);
        if (this.currentIterationNumber == null)
            Verify.verify(currentIterationNumber == 1);
        else
            Verify.verify(currentIterationNumber == this.currentIterationNumber + 1);
        this.currentIterationNumber = currentIterationNumber;
    }


    /**
     * Should be called by external logic when starting a new inference.
     * Reminder: sempre may invoke two consecutive inferences on the same example.
     */
    public void startNewInference(edu.stanford.nlp.sempre.Example sempreExample) {
        Example example = exampleIdToExample.getExisting(sempreExample.id);
        currentInferenceData = new InferenceData(settings, example, sempreExample);
        analysis.reportStartingNewInference(sempreExample);
    }

    public void endInference() {
        analysis.logMemoryUsageIfRelevant();
        featureNameStorageOfCurrentInference.clear();
    }


    public void newDatasetGroupBegins(String groupLabel) {
        currentDatasetGroupLabel = DatasetSempreGroupLabel.getFromSempreTag(groupLabel);
        analysis.reportNewDatasetGroupBegins(currentDatasetGroupLabel);
    }




    /**
     * To be called when Sempre tries to do an update.
     * @param originalUpdate the update that Sempre originally intended to add to the weight.
     * @return the (possibly modified) update to be carried out.
     */
    public double calcWeightUpdate(String featureName, double originalUpdate) {
        Verify.verify(getCurrentDatasetGroupLabel() == DatasetSempreGroupLabel.TRAIN);

        if (conditionalWeightUpdater == null)
            return originalUpdate;
        
        return conditionalWeightUpdater.calcWeightUpdate(featureName, getCurrentExampleBeingParsed().getDomain(),
                originalUpdate);
    }

    /**
     * Should be called immediately after Sempre calculates the gradient of the objective.
     * @param gradient a map from feature name to the element of its weight in the gradient vector. May be modified by
     *                 this method.
     */
    public void postprocessGradient(Map<String, Double> gradient) {
        if (featuresToOptimizeWeightsFor != null){
            gradient.keySet().stream()
                    .filter(feature->!featuresToOptimizeWeightsFor.safeContains(feature))
                    .collect(Collectors.toList()) // this is necessary because you may not modify the set you're
                                                  // iterating on.
                    .forEach(feature -> {
                        Double value = gradient.remove(feature);
                        Verify.verify(value != null);
                    });
        }
    }


    public void setSempreParser(Parser sempreParser) {
        this.sempreParser = sempreParser;
    }


}
