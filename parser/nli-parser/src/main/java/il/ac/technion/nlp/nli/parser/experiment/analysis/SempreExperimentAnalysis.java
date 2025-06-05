package il.ac.technion.nlp.nli.parser.experiment.analysis;

import com.google.common.base.Verify;
import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import ofergivoli.olib.general.MemoryAnalyzer;
import ofergivoli.olib.io.GeneralFileUtils;
import ofergivoli.olib.io.TextIO;
import ofergivoli.olib.io.log.Log;
import ofergivoli.olib.io.log.LogDirectory;
import ofergivoli.olib.io.log.Logger;
import ofergivoli.olib.io.serialization.xml.XStreamSerialization;
import edu.stanford.nlp.sempre.*;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.parser.experiment.analysis.results.InferenceResults;
import il.ac.technion.nlp.nli.parser.experiment.analysis.results.SempreExperimentResults;
import il.ac.technion.nlp.nli.parser.general.DatasetSempreGroupLabel;
import il.ac.technion.nlp.nli.parser.general.SempreExperiment;
import il.ac.technion.nlp.nli.parser.NliMethodCallFormula;
import il.ac.technion.nlp.nli.parser.denotation.LazyStateValue;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

/**
 * All the analysis and results data associated with an {@link SempreExperiment}.
 * Meant to be saved persistently as file tree (hence not serializable).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class SempreExperimentAnalysis {

    public static final Path predictedLogicalFormsOnTest_rootPathRelativeToAnalysisDir =
            Paths.get("predictedLogicalForms/test");

    public static final Path predictedFunctionCallOnTest_rootPathRelativeToAnalysisDir =
            Paths.get("predictedFunctionCalls/test");


    /**
     * TODO: move other relevant fields into this one.
     */
    private final ExperimentAnalysisSettings settings;


    /**
     * When null, not being updated.
     */
    @Nullable private Long lastRecordedUsedMemoryByJmvInBytes;


    public boolean writeExtractedInstructionFeaturesData = true; //TODO: move to ExperimentAnalysisSettings

    private final File outputAnalysisDir;
    public final SempreExperimentResults experimentResults;
    private final SempreExperiment sempreExperiment;
    /**
     * The value is the examples that the feature (key) was extracted from (throughout all train/test iterations).
     * Null if not used.
     */
    private @Nullable SafeMap<String, Set<Example>> instructionFeatureIdToExamples = null;

    /**
     * If not null, contains a map from feature and domain to the sum of all partial gradients of the feature's weights
     * over all training examples from the domain.
     */
    public @Nullable SafeMap<Pair<String,Domain>,Double> featureAndDomainToGradientSum;
    public @Nullable Example currentSempreExampleBeingParsed;


    private final LogDirectory logDir;
    private final LogDirectory derivationDepthOrSizeLogDir;

    /**
     * Being set by {@link #reportStartingNewInference}
     */
    public @Nullable Logger allDerivationsOfCurrentExampleLogger;


    /**
     * Being set by {@link #reportStartingNewInference}
     */
    public @Nullable Logger memoryLogger;
    private int reportedDerivationNumber;


    /**
     * @param sempreExperiment this constructor may be called from the constructor of 'sempreExperiment' (we only save reference to
     *                   it here).
     * @param outputAnalysisDir If not existing already, created along with all missing directories on its path.
     */
    public SempreExperimentAnalysis(SempreExperiment sempreExperiment, ExampleSplit split,
                                    ExperimentAnalysisSettings settings, File outputAnalysisDir) {
        this.settings = settings;

        GeneralFileUtils.createDirectories(outputAnalysisDir.toPath());
        this.outputAnalysisDir = outputAnalysisDir;
        this.logDir = new LogDirectory(this.outputAnalysisDir);

        if (settings.logDerivationDepthOrSize)
            this.derivationDepthOrSizeLogDir = new LogDirectory(this.outputAnalysisDir.toPath().
                resolve("derivation_" + getSizeOrDepthLabel()));
        else
            this.derivationDepthOrSizeLogDir = null;

        if (writeExtractedInstructionFeaturesData)
            instructionFeatureIdToExamples = new SafeHashMap<>();

        this.sempreExperiment = sempreExperiment;

        experimentResults = new SempreExperimentResults(split);

        if (settings.buildFeatureAndDomainToGradientSumMap)
            this.featureAndDomainToGradientSum = new SafeHashMap<>();

    }

    /**
     * Should be called whenever adding an instruction feature.
     */
    public void reportExtractedInstructionFeatureNotFilteredOut(Example example,
                                                                @SuppressWarnings("unused") Derivation deriv,
                                                                String feature) {
        if (instructionFeatureIdToExamples != null) {
            if (!instructionFeatureIdToExamples.safeContainsKey(feature)) {
                instructionFeatureIdToExamples.put(feature, new HashSet<>());
                // Logging some info during runtime in case many instruction features are extracted and we might crash
                // due to memory limit:
                int uniqueFeaturesNumReportedSoFar = instructionFeatureIdToExamples.size();
                if (uniqueFeaturesNumReportedSoFar % 10000 == 0)
                    Log.trace("unique feature ids reported so far=" + uniqueFeaturesNumReportedSoFar);
                if (uniqueFeaturesNumReportedSoFar % 500000 == 0) {
                    Log.trace("Writing instruction features extracted so far");
                    writeExtractedInstructionFeaturesCsvAndListIfRelevant();
                }
            }
            instructionFeatureIdToExamples.safeGet(feature).add(example);
        }

    }




    public void writeExtractedInstructionFeaturesCsvAndListIfRelevant() {

        if (instructionFeatureIdToExamples == null)
            return;

        File outCsv = new File(outputAnalysisDir, "extractedInstructionFeaturesWithExamplesCounter.csv");
        File outFeaturesListFile = new File(outputAnalysisDir, "extractedInstrcutionFeatures.txt");

        try (Writer csvWriter = TextIO.getStreamWriterForUtf8(outCsv, false);
             Writer listWriter = TextIO.getStreamWriterForUtf8(outFeaturesListFile, false)) {
            instructionFeatureIdToExamples.entrySet().stream()
                    .sorted((x,y)->-1*Integer.compare(x.getValue().size(),y.getValue().size()))
                    .forEach(entry->{
                        String featureId = entry.getKey();
                        Integer examplesNum = entry.getValue().size();
                        try {
                            csvWriter.write(StringEscapeUtils.escapeCsv(featureId) + "," + examplesNum + "\n");
                            listWriter.write(featureId + "\n");
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    public void reportSempreStarting() {

        if (Builder.opts.inParamsPath == null)
            logDir.getGeneralLogger().log("not loading pre-trained weights");
        else
            logDir.getGeneralLogger().log("LOADING PRE-TRAINED WEIGHTS: " + Builder.opts.inParamsPath);


        if(!edu.stanford.nlp.sempre.Dataset.opts.maxExamples.isEmpty())
            logDir.getGeneralLogger().log("NOTE: The edu.stanford.nlp.sempre.Dataset.opts.maxExamples option was set");
        else {
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n\n\n----Training Examples----\n");
            sempreExperiment.trainTestSplit.getTrainExamples().forEach(ex ->
                    sb.append(ex.getDomain())
                            .append("\t")
                            .append(ex.getId())
                            .append("\t")
                            .append(ex.getInstructionUtterance())
                            .append("\n"));

            sb.append("\n\n\n\n----Test Examples----\n");
            sempreExperiment.trainTestSplit.getTestExamples().forEach(ex ->
                    sb.append(ex.getDomain())
                            .append("\t")
                            .append(ex.getId())
                            .append("\t")
                            .append(ex.getInstructionUtterance())
                            .append("\n"));
            logDir.getGeneralLogger().log(sb.toString());
        }

    }

    public void reportSempreFinished() {
        writeFeatureAndDomainToGradientSum();
        writeExtractedInstructionFeaturesCsvAndListIfRelevant();
        writeExampleCorrectnessFromTestOfLastIteration();
    }

    private void writeCwuAnalysisDataAtEndOfTrainingIterationIfRelevant() {
        if (sempreExperiment.conditionalWeightUpdater != null &&
                sempreExperiment.settings.analysisSettings.collectAggregatedAnalysisDataForCWU) {
            //noinspection ConstantConditions
            String iterationStr = "it" + + sempreExperiment.getCurrentIterationNumber();
            TextIO.writeTextToFileInStandardEncoding(
                    logDir.getOutputDirectory().resolve("CWU__AggregatedAnalysisData__" + iterationStr + ".tsv"),
                    sempreExperiment.conditionalWeightUpdater.getAggregatedAnalysisDataString(), false);
        }
    }

    private void writeFeatureAndDomainToGradientSum() {
        if (featureAndDomainToGradientSum == null)
            return;

        Path featureAndDomainsCsv = outputAnalysisDir.toPath().resolve("featureAndDomainToGradientSum.xml");
        XStreamSerialization.writeObjectToXmlFile(featureAndDomainToGradientSum, featureAndDomainsCsv);
    }

    /**
     * Writes to a file the correctness value (in the range [0,1]) of the examples in dev/test in the final iteration.
     * The order of the examples written matches the chronological order of inference.
     */
    private void writeExampleCorrectnessFromTestOfLastIteration() {
        Logger logger = logDir.getOrCreateLogger("correctnessOfExamplesFromTestOfLastIteration");
        sempreExperiment.trainTestSplit.getTestExamples().forEach(ex-> {
            InferenceResults results = this.experimentResults.getLastIterationResults().getInferenceResults(ex);
            if (results!=null)
                logger.log(ex.getId() + "\t" + results.correct);
        });
    }

    private final String allInferenceResultsLoggerId = "allInferenceResults";

    /**
     * To be called from external logic.
     * This method should be called by Sempre whenever an inference finishes.
     * @param timeInMs the time the inference took in milliseconds.
     * @param correct in the range [0,1]
     */
    public void processInferenceResults(Example ex, long timeInMs, double correct) {
        InferenceResults results = new InferenceResults(correct, timeInMs/1000.0);

        il.ac.technion.nlp.nli.core.dataset.Example nliExample = sempreExperiment.exampleIdToExample.safeGet(ex.id);
        //noinspection ConstantConditions
        this.experimentResults.addInferenceResults(sempreExperiment.getCurrentIterationNumber(), nliExample, results);

        logDir.getOrCreateLogger(allInferenceResultsLoggerId).log(
                nliExample.getId() + "\t" + results.correct + "\t" + results.time);
    }

    public void reportNewDatasetGroupBegins(DatasetSempreGroupLabel currentDatasetGroupLabel) {
        logDir.getOrCreateLogger(allInferenceResultsLoggerId).log(
                "iteration " + sempreExperiment.getCurrentIterationNumber()
                        + "\t" + currentDatasetGroupLabel.tag);
        logDir.getOrCreateLogger(allInferenceResultsLoggerId).log("id\tcorrect\ttime");

    }

    public void reportDatasetGroupEnds(DatasetSempreGroupLabel datasetGroupLabel) {
        if (datasetGroupLabel == DatasetSempreGroupLabel.TRAIN){
            writeCwuAnalysisDataAtEndOfTrainingIterationIfRelevant();
        }
    }


    public void reportAbortingCurrentInference() {
        logDir.getWarningLogger().log(
                "Aborting inference for example " + sempreExperiment.getCurrentExampleBeingParsed().getId());
    }


    /**
     * Do be called during inference after detecting the memory usage decreased.
     */
    public void reportGarbageCollection(){
        getMemoryLogger().log("Garbage collection detected. Memory used currently (MiB):\t" +
                        MemoryAnalyzer.getUsedMemoryByJvmInMiBAsFormattedStr());

    }

    private Logger getMemoryLogger() {
        return logDir.getOrCreateLogger("memory");
    }

    /**
     * @param gradient a map from feature f to the partial derivation of the objective with respect to the weight of f.
     */
    public void reportWeightsUpdateBegins(Map<String, Double> gradient) {

        Verify.verify(sempreExperiment.getCurrentDatasetGroupLabel() == DatasetSempreGroupLabel.TRAIN);

        SafeMap<Pair<String, Domain>, Double> gradMap = sempreExperiment.analysis.featureAndDomainToGradientSum;
        if (gradMap != null){
            Domain domain = sempreExperiment.getCurrentExampleBeingParsed().getDomain();
            gradient.forEach((feature,grad)-> {
                Pair<String, Domain> featureDomainPair = new ImmutablePair<>(feature, domain);
                if (!gradMap.safeContainsKey(featureDomainPair))
                    gradMap.put(featureDomainPair, 0.0);
                gradMap.put(featureDomainPair, gradMap.safeGet(featureDomainPair) + grad);
            });
        }
    }


    public void reportWeightsUpdateEnds(@SuppressWarnings("unused") Params weights) {
        // currently not used.
    }


    /**
     * @param derivations The candidate derivations predicted during inference. Ordered by descending score.
     * @param derivationFeatureValuesSupplier each element provides the extracted features for the derivation with that
     *                                        index. Note: it is a supplier for performance reasons (i.e. perhaps it
     *                                        won't be used).
     * @param topDerivationsNumber number of derivations with maximum score.
     */
    public void reportDerivationsPredictedByInference(
            Example example, ArrayList<Derivation> derivations, ArrayList<Boolean> derivationsCorrectness,
            Supplier<ArrayList<List<FeatureAndValue>>> derivationFeatureValuesSupplier,
            ArrayList<Double> derivationsScores, int topDerivationsNumber, ParserState sempreParserState) {


        @SuppressWarnings("ConstantConditions")
        int currentIterationNumber = sempreExperiment.getCurrentIterationNumber();

        if (settings.saveLogicalFormsOfTopPredictedDerivationsForTestInferences &&
                sempreExperiment.getCurrentDatasetGroupLabel() != DatasetSempreGroupLabel.TRAIN){

                List<CandidateLogicalForm> candidateLogicalForms = new ArrayList<>();
                for (int i = 0; i < topDerivationsNumber; i++) {
                    ArrayList<List<FeatureAndValue>> derivationFeatureValues =  derivationFeatureValuesSupplier.get();
                    candidateLogicalForms.add(new CandidateLogicalForm(
                            (NliMethodCallFormula) derivations.get(i).formula,
                            derivationsCorrectness.get(i), derivationsScores.get(i), derivationsScores.get(0),
                            sempreParserState.params, derivationFeatureValues.get(i)));
                }

            Path outputXml = outputAnalysisDir.toPath()
                        .resolve(predictedLogicalFormsOnTest_rootPathRelativeToAnalysisDir)
                        .resolve("iteration_" + currentIterationNumber)
                        .resolve(example.id + ".xml");
                GeneralFileUtils.createDirectories(outputXml.getParent()); // may already exist.
                XStreamSerialization.writeObjectToXmlFile(candidateLogicalForms, outputXml);
        }


        if (settings.saveFunctionCallsOfTopPredictedDerivationsForTestInferences &&
                sempreExperiment.getCurrentDatasetGroupLabel() != DatasetSempreGroupLabel.TRAIN) {

            List<MethodCall> methodCalls = new ArrayList<>();
            for (int i = 0; i < topDerivationsNumber; i++) {
                List<Value> values = ((ListValue) derivations.get(i).value).values;
                Verify.verify(values.size()==1);
                methodCalls.add(((LazyStateValue) values.get(0)).getMethodCall());
            }
            Path outputXml = outputAnalysisDir.toPath()
                    .resolve(predictedFunctionCallOnTest_rootPathRelativeToAnalysisDir)
                    .resolve("iteration_" + currentIterationNumber)
                    .resolve(example.id + ".xml");
            GeneralFileUtils.createDirectories(outputXml.getParent()); // may already exist.
            XStreamSerialization.writeObjectToXmlFile(methodCalls, outputXml);
        }

        if (settings.saveAllCandidateLogicalFormsWithTheirFeatureData && !sempreExperiment.isFirstStep &&
                sempreExperiment.getCurrentDatasetGroupLabel() != DatasetSempreGroupLabel.TRAIN
                && currentIterationNumber == sempreExperiment.settings.iterationsNum) {

            ArrayList<List<FeatureAndValue>> derivationFeatureValues =  derivationFeatureValuesSupplier.get();
            Path outputDir = getOutputAnalysisDirectoryOfExampleBeingParsed()
                    .resolve("allCandidateLogicalFromsWithTheirFeatureData");
            GeneralFileUtils.createDirectories(outputDir);
            for (int i = 0; i < derivations.size(); i++) {
                CandidateLogicalForm candidateLogicalForm = new CandidateLogicalForm(
                        (NliMethodCallFormula) derivations.get(i).formula,
                        derivationsCorrectness.get(i), derivationsScores.get(i), derivationsScores.get(0),
                        sempreParserState.params, derivationFeatureValues.get(i));
                boolean isTopDerivation = i < topDerivationsNumber;
                String fileNameSuffix = isTopDerivation ? "__top" : "";
                if (candidateLogicalForm.correctDenotation)
                    fileNameSuffix = fileNameSuffix + "__CORRECRT_DENOTATION";
                String fileName = String.format("%03d" + fileNameSuffix + ".txt",i);
                String depthOrSizeLabel = getSizeOrDepthLabel();
                String fileContent = "instruction: " + example.utterance + "\n" +
                        depthOrSizeLabel + ": " + sempreParserState.getMinDepthOrSizeOfDerivation(derivations.get(i)) + "\n" +
                        candidateLogicalForm.toString();
                TextIO.writeTextToFileInStandardEncoding(outputDir.resolve(fileName).toFile(),
                        fileContent, false);
            }
        }

        if (settings.logDerivationDepthOrSize) {
            OptionalDouble average = derivations.stream().mapToInt(sempreParserState::getMinDepthOrSizeOfDerivation)
                    .average();
            derivationDepthOrSizeLogDir.getOrCreateLogger("average__iteration_" + currentIterationNumber +
                    "__" + sempreExperiment.getCurrentDatasetGroupLabel().toString().toLowerCase())
                    .log("" + (average.isPresent() ? average.getAsDouble() : -1));
        }
    }

    private static String getSizeOrDepthLabel() {
        return FloatingParser.opts.useSizeInsteadOfDepth ?  "size" : "depth";
    }


    public void logMemoryUsageIfRelevant() {
    if (!settings.logMemoryUsage)
            return;


        getMemoryLogger().log("example: " + Objects.requireNonNull(currentSempreExampleBeingParsed).id);
        getMemoryLogger().log("estimated memory used for storing feature data (MiB): " +
                MemoryAnalyzer.getMemoryInMiBAsFormattedStr(
                        sempreExperiment.featureNameStorageOfCurrentInference
                                .getEstimatedMemoryUsedForStoringFeatureDataInBytes()));

        long currentlyUsedMemoryByJvmInBytes = MemoryAnalyzer.getUsedMemoryByJvmInBytes();
        if (lastRecordedUsedMemoryByJmvInBytes != null &&
                // If the following fulfills the GC probably just freed memory:
                currentlyUsedMemoryByJvmInBytes < lastRecordedUsedMemoryByJmvInBytes){
            sempreExperiment.analysis.reportGarbageCollection();
        }
        lastRecordedUsedMemoryByJmvInBytes = currentlyUsedMemoryByJvmInBytes;
    }

    public void reportNewDerivationCreatedByRule(Rule rule, String cellName, Derivation deriv, boolean pruned) {
        if (settings.saveAllDerivations) {

            StringBuilder sb = new StringBuilder();


            reportedDerivationNumber++;


            sb.append("cellName=" + cellName)
                    .append("\t\t\t\treportedDerivationNumber=" + reportedDerivationNumber).append("\n");
            sb.append(rule.toString()).append("\n");
            sb.append(GeneralAnalysisUtils.getHumanFriendlyRepresentationOfFormula(deriv.formula))
                    .append("\t\t\t=\t\t\t").append(deriv.formula.toString()).append("\n");
            if (pruned)
                sb.append("************** PRUNED **************\n");
            Path outputDir = getOutputAnalysisDirectoryOfExampleBeingParsed();
            GeneralFileUtils.createDirectories(outputDir);
            Objects.requireNonNull(allDerivationsOfCurrentExampleLogger).log(sb.toString());
        }
    }



    private Path getOutputAnalysisDirectoryOfExampleBeingParsed(){
        return outputAnalysisDir.toPath()
                .resolve(sempreExperiment.getCurrentDatasetGroupLabel().tag)
                .resolve("iteration_" + sempreExperiment.getCurrentIterationNumber())
                .resolve(Objects.requireNonNull(currentSempreExampleBeingParsed).id);
    }

    public void reportStartingNewInference(Example sempreExample) {

        currentSempreExampleBeingParsed = sempreExample;
        reportedDerivationNumber = 0;
        if (settings.saveAllDerivations)
            allDerivationsOfCurrentExampleLogger = new Logger(
                    getOutputAnalysisDirectoryOfExampleBeingParsed().resolve("allDerivations.log").toFile(), true);
    }


}
