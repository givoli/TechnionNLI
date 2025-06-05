package il.ac.technion.nlp.nli.parser.experiment.analysis.results;

import com.google.common.base.Verify;
import ofergivoli.olib.io.csv.CsvContent;
import ofergivoli.olib.io.csv.CsvDataCell;
import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;
import il.ac.technion.nlp.nli.parser.EnvironmentSettings;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentSettings;
import il.ac.technion.nlp.nli.parser.experiment.analysis.SempreExperimentAnalysis;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ExperimentAnalysisCsvRow {

    /**
     * Order of this list will determine the order of header cells.
     */
    private List<CsvDataCell> headerCellAndContentCellList = new ArrayList<>();

    /**
     * For experiments without 2-step training.
     */
    public ExperimentAnalysisCsvRow(int experimentNumber, ExperimentSettings settings,
                                    SempreExperimentAnalysis analysis){
        addAllCommonFirstPairs(experimentNumber, settings);
        addAnalysisPairs(settings, analysis, null);
    }


    /**
     * For experiments with 2-step training.
     */
    public ExperimentAnalysisCsvRow(int experimentNumber, ExperimentSettings settings,
                                    SempreExperimentAnalysis step1Analysis, SempreExperimentAnalysis step2Analysis){

        Verify.verify(settings.twoStepTraining);

        addAllCommonFirstPairs(experimentNumber, settings);
        addAnalysisPairs(settings, step1Analysis, 1);
        addAnalysisPairs(settings, step2Analysis, 2);

    }

    /**
     * @param hash The hash of the commit of the git repository containing {@link EnvironmentSettings#moduleResourceDir}.
     * A null value represents the a situation in which the local repository changed relative to the head commit.
     */
    public void addGitCommitHash(@Nullable String hash) {
        if (hash==null)
            hash = "";
        add("gitCommit", hash);
    }

    /**
     * @param stepNumber should be null if it's not a 2-step experiment, otherwise should be either 1 or 2.
     */
    private void addAnalysisPairs(ExperimentSettings settings, SempreExperimentAnalysis analysis,
                                  @Nullable Integer stepNumber) {

        boolean isFirstStepExperiment = (stepNumber != null) && (stepNumber == 1);

        String headerCellPrefix = isFirstStepExperiment ? "step1_" : "";

        //noinspection ConstantConditions
        int iterationsNum = isFirstStepExperiment ? settings.firstStepIterationsNum : settings.iterationsNum;

        for (int i = 1; i<= iterationsNum; i++)
            add(headerCellPrefix + "trainAcc_it" + i, analysis.experimentResults.getAccuracy(ExampleSplit.SplitPart.TRAIN, i));
        for (int i = 1; i<= iterationsNum; i++)
            add(headerCellPrefix + "testAcc_it" + i, analysis.experimentResults.getAccuracy(ExampleSplit.SplitPart.TEST, i));

        add(headerCellPrefix + "meanTimePerInference_firstIt",
                analysis.experimentResults.getIterationResults(1).getAverageInferenceTime());

        add(headerCellPrefix + "meanTimePerInference_lastIt",
                analysis.experimentResults.getLastIterationResults().getAverageInferenceTime());
    }


    /**
     * For adding pairs that appear once even if using the 2-step method.
     */
    private void addAllCommonFirstPairs(int experimentNumber, ExperimentSettings settings) {
        add("experimentNum", experimentNumber);

        add("testDomainId", settings.testDomainId);

        add("setupType", settings.setupType);


        // adding settings.tagNameToValue, ordered by tag name:
        settings.tagNameToValue.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry->add(entry.getKey(),entry.getValue()));

        add("shuffleTrainingExamplesBetweenIterations", settings.shuffleTrainingExamplesBetweenIterations);
        add("regularizationCoefficient", settings.regularizationCoefficient);
        add("initStepSize", settings.initStepSize);
        add("lazyL1FullUpdateFreq", settings.lazyL1FullUpdateFreq);
        add("beamSize", settings.beamSize);
        add("useAppLogicFiltering", settings.useAppLogicFiltering);
        add("useLexicalizedPhrasePredicateFeatures", settings.useLexicalizedPhrasePredicateFeatures);
        add("usePredicatesThatAreProbablyDomainSpecificForExtractingLexicalizedPhrasePredicateFeatures", settings.usePredicatesThatAreProbablyDomainSpecificForExtractingLexicalizedPhrasePredicateFeatures);
        add("useDescriptionPhraseFeatures", settings.useDescriptionPhraseFeatures);
        add("unconditionalWeightUpdateFraction", settings.unconditionalWeightUpdateFraction);
        add("domainsNumberRequiredForCwu", settings.domainsNumberRequiredForCwu);

        add("trainingExamplesNumber", settings.trainingExampleIds.size());
        add("testExamplesNumber", settings.testExampleIds.size());

        add("initialWeightsFile", settings.initialWeightsFileAbsoluteOrRelativeToExperimentDir);

        add("twoStepTraining",settings.twoStepTraining);

        add("firstStepRegularizationCoefficient", settings.firstStepRegularizationCoefficient);
        add("firstStepInitStepSize", settings.firstStepInitStepSize);

        add("firstStepTrainingDomainIds", settings.firstStepTrainingDomainIds);
        add("firstStepTrainingDomainsNum", settings.firstStepTrainingDomainIds==null ? "" :
                settings.firstStepTrainingDomainIds.size());
        add("firstStepIterationsNum", settings.firstStepIterationsNum);
        add("numOfFeaturesToUpdateWeightsForInStep2", settings.numOfFeaturesToUpdateWeightsForInStep2);

        add("featuresAllowedToBeExtractedNum", settings.featuresAllowedToBeExtractedNum);
    }




    private void add(String headerCell, String contentCell){
        headerCellAndContentCellList.add(new CsvDataCell(headerCell,contentCell));
    }

    private void add(Object headerCell, @Nullable Object contentCell){
        String contentCellStr = contentCell==null ? "" : contentCell.toString();
        add(headerCell.toString(), contentCellStr);
    }


    public static CsvContent getAsCsvContent(List<ExperimentAnalysisCsvRow> rows){
        return CsvContent.createFromValuePairs(rows.stream()
                .map(experimentAnalysisCstRow->experimentAnalysisCstRow.headerCellAndContentCellList)
                .collect(Collectors.toList()));
    }

}
