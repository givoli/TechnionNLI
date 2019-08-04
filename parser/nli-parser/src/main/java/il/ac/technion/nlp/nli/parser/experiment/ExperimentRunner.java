package il.ac.technion.nlp.nli.parser.experiment;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import com.ofergivoli.ojavalib.general.CommandRunner;
import com.ofergivoli.ojavalib.io.GeneralFileUtils;
import com.ofergivoli.ojavalib.io.serialization.SerializationUtils;
import com.ofergivoli.ojavalib.io.serialization.xml.XStreamSerialization;
import com.ofergivoli.ojavalib.time.TemporalFormat;
import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.tables.DPParser;
import edu.stanford.nlp.sempre.tables.features.PhrasePredicateFeatureComputer;
import fig.exec.Execution;
import il.ac.technion.nlp.nli.core.dataset.Dataset;
import il.ac.technion.nlp.nli.core.dataset.Example;
import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;
import il.ac.technion.nlp.nli.parser.EnvironmentSettings;
import il.ac.technion.nlp.nli.parser.experiment.analysis.FeatureGeneralityTools;
import il.ac.technion.nlp.nli.parser.experiment.analysis.SempreExperimentAnalysis;
import il.ac.technion.nlp.nli.parser.experiment.analysis.results.ExperimentAnalysisCsvRow;
import il.ac.technion.nlp.nli.parser.general.SempreExperiment;
import il.ac.technion.nlp.nli.parser.InstructionValueEvaluator;
import il.ac.technion.nlp.nli.parser.SempreInstructionDataset;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Technical notes regarding Sempre's output:
 * If you run multiple experiments (sequentially):
 * - some files under Sempre's output dir are being overwritten in each sempreExperiment if not moved by this class,
 * these includes: 'params.*'
 * - some files under Sempre's output dir are appended to (and never cleared) in each sempreExperiment, these
 * includes:
 * - log
 * - learner.events
 * - preds-iter*-*.examples
 * - The file 'output.map' only gets filled with useful content when the program exits (and then it probably
 * refers to the last iteration only).
 * - Also, some files in Sempre's output directory can't be deleted until Sempre exists, e.g. the "log" file.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ExperimentRunner {

    /**
     * Runs an experiment.
     * The working directory must be the Sempre Directory (because Sempre users this assumption to fetch files).
     *
     * Parameters:
     *  param 1: Path of the resource directory of this module.
     *  param 2: Path of the dataset ser file.
     *  param 3: Path to an experiment directory (to be run).
     *
     * Note: Sempre prints a lot of text to stdout, and some (general information) text to stderr.
     */
    public static void main(String[] args) {

        if (!Files.exists(Paths.get("module-classes.txt")))
            throw new RuntimeException("The current working directory must be the Sempre Data directory.");


        Path moduleResourceDir = Paths.get(args[0]);
        if (!Files.isDirectory(moduleResourceDir))
            throw new RuntimeException("Module resource directory does not exist: " + moduleResourceDir);

        Path datasetSer = Paths.get(args[1]);
        if (!Files.exists(datasetSer))
            throw new RuntimeException("Dataset file does not exist: " + datasetSer);

        EnvironmentSettings environmentSettings = new EnvironmentSettings(moduleResourceDir, datasetSer);
        ExperimentDirectory experimentDirectory = new ExperimentDirectory(Paths.get(args[2]));


        Path sempreDataDir = environmentSettings.moduleResourceDir.resolve(
                environmentSettings.sempreDataDirRelativeToResourceDir);
        File sempreArgsFile = sempreDataDir.resolve(
                environmentSettings.sempreArgsXmlFileRelativeToSempreDataDir).toFile();
        String[] sempreArgs = XStreamSerialization.readObjectFromTrustedXmlFile(false, sempreArgsFile);


        Path sempreDirectoriesParent = GeneralFileUtils.getHomeDir().resolve(
                environmentSettings.tempSempreDirectoriesParentRelativeToHomeDir);
        setSempreOutputDirectoryInSempreArgsAndCreateDirectory(sempreDirectoriesParent, sempreArgs);


        Runnable runnable = ()->{
            ExperimentRunner experimentRunner = new ExperimentRunner(environmentSettings);
            Dataset dataset = SerializationUtils.readObjectFromFile(environmentSettings.datasetSer.toFile());
            experimentRunner.runExperiment(dataset, true, experimentDirectory);
        };

        /**
         * Note: the "Main" argument allows for setting Sempre options to the {@link edu.stanford.nlp.sempre.Main}
         * class.
         */
        Execution.run(sempreArgs, "Main", runnable, Master.getOptionsParser());

    }


    private static int runsCounter = 0;

    private final EnvironmentSettings environmentSettings;

    public ExperimentRunner(EnvironmentSettings environmentSettings) {
        this.environmentSettings = environmentSettings;
    }





    /**
     * Creates a new temporary directory in 'sempreTempDirectoriesParent' (with any missing directories along it's
     * path), and then modifies 'sempreArgs' to use the newly created directory as Sempre's temp directory.
     * @param sempreTempDirectoriesParent The path may or may not already exist.
     * @param sempreArgs Modified by this method: the temp directory to be used by Sempre is set (replacing old one).
     */
    public static void setSempreOutputDirectoryInSempreArgsAndCreateDirectory(Path sempreTempDirectoriesParent,
                                                                               String[] sempreArgs) {

        Path sempreTmpDir;
        long count = 1;
        do { // until we create a non-existing directory.
            String name = TemporalFormat.getCurrentTimeInFullTimeForFilenameFormat() + "_" + count;
            sempreTmpDir = sempreTempDirectoriesParent.resolve(name);
        } while (!sempreTmpDir.toFile().mkdirs());

        // replacing the argument in 'sempreArgs':
        for (int i=0; i<sempreArgs.length; i++) {
            if (sempreArgs[i].equals("-execDir") && i+1<sempreArgs.length){
                Verify.verify(sempreArgs[i+1].equals("__SET_IN_RUNTIME__"));
                sempreArgs[i+1] = sempreTmpDir.toAbsolutePath().toString();
                return;
            }
        }

        throw new RuntimeException("bad sempreArgs - no '-execDir'");
    }



    /**
     * The sempreExperiment currently running or null if no {@link SempreExperiment} is currently running.
     * (So the current running {@link SempreExperiment} could be accessed from Sempre's code)
     */
    private static @Nullable SempreExperiment currentRunningSempreExperiment;


    /**
     * Turns to 'true' (forever) once the first {@link SempreExperiment} is run (in the lifetime of the running process).
     */
    private static boolean alreadyInvokedAnExperiment = false;




    /**
     *
     * The working directory of the current java process must be the Sempre Directory (because Sempre relies on this
     * to be true when fetching files).
     *
     * Important Notes:
     * - Running Sempre multiple times simultaneously in the same process is not permitted (Sempre can't be trusted to
     *    be thread-safe).
     * - Running Sempre multiple times sequentially in the same process might yield non-deterministic results (i.e.
     *   deterministic only given the execution sequence), due static {@link Random} fields in Sempre.
     * - This method modified static Options fields of Sempre's classes.
     * - Sempre's output directory is first archived if not already empty.
     *
     * @param experimentDir the directory containing experiment settings and the one to which the output will be written
     *                      to.
     */
    public void runExperiment(Dataset dataset, boolean preprocessAndModifyDataset, ExperimentDirectory experimentDir) {

        if (++runsCounter>1)
            throw new RuntimeException("Multiple runs not yet supported"); // TODO: check that it's ok to do multiple runs. Then you can delete the 'runsCounter' field.

       if(Files.exists(experimentDir.getOutputDirectory()))
           throw new RuntimeException("Output directory already exists: " + experimentDir);


        if (currentRunningSempreExperiment != null)
            throw new RuntimeException("A SempreExperiment is already running!");

        if (preprocessAndModifyDataset)
            dataset.preprocessExamples();

        int experimentNum;
        try {
            experimentNum = Integer.parseInt(experimentDir.getExperimentId());
        } catch (NumberFormatException e){
            experimentNum = -1;
        }

        ExperimentSettings experimentSettings;
        experimentSettings = experimentDir.readExperimentSettings(false);

        if (experimentSettings.testOnTrainSetAfterAllIterations) {
            throw new RuntimeException("not yet supported"); //TODO
        }

        List<Example> trainExamples = experimentSettings.trainingExampleIds.stream()
                .map(dataset::getExampleById).collect(Collectors.toList());
        List<Example> testExamples = experimentSettings.testExampleIds.stream()
                .map(dataset::getExampleById).collect(Collectors.toList());
        ExampleSplit split = new ExampleSplit(trainExamples, testExamples);


        if (!alreadyInvokedAnExperiment) {
            verifyIgnoredSempreOptionsAreNotSet();
            alreadyInvokedAnExperiment = true;
        }


        @Nullable String gitCommitHash = getHashOfHeadCommitIfLocalGitRepositoryDidNotChange();

        @Nullable Path initialWeightsFile = null;
        if (experimentSettings.initialWeightsFileAbsoluteOrRelativeToExperimentDir != null) {
            initialWeightsFile = experimentDir.getPath().resolve(
                    experimentSettings.initialWeightsFileAbsoluteOrRelativeToExperimentDir.toPath());
        }

        ExperimentAnalysisCsvRow csvRow;
        if (experimentSettings.twoStepTraining &&
                experimentSettings.setupType != ExperimentSettings.SetupType.EVALUATING_ONLY) {

            @SuppressWarnings("ConstantConditions")
            ExampleSplit firstStepSplit = split.getSubsetByFilteringExamples(ExampleSplit.SplitPart.TRAIN,
                    ex->experimentSettings.firstStepTrainingDomainIds.contains(ex.getDomain().getId()));
            @SuppressWarnings("ConstantConditions")
            ExampleSplit secondStepSplit = split.getSubsetByFilteringExamples(ExampleSplit.SplitPart.TRAIN,
                    ex->!experimentSettings.firstStepTrainingDomainIds.contains(ex.getDomain().getId()));

            @SuppressWarnings("ConstantConditions")
            @Nullable SafeSet<String> featuresToOptimizeForInStep2;
            if (experimentSettings.numOfFeaturesToUpdateWeightsForInStep2 == null)
                featuresToOptimizeForInStep2 = null;
            else {
                //noinspection ConstantConditions
                featuresToOptimizeForInStep2 = FeatureGeneralityTools.getFeaturesByGeneralityScoreRank(
                        experimentSettings.numOfFeaturesToUpdateWeightsForInStep2,
                        environmentSettings.moduleResourceDir.resolve(
                                experimentSettings.featuresGeneralityScoresXml.toPath()));
            }

            SempreExperimentAnalysis analysis1 = runSempreExperiment(
                    experimentSettings, firstStepSplit, experimentDir, 1, initialWeightsFile, null,
                    true);
            SempreExperimentAnalysis analysis2 = runSempreExperiment(
                    experimentSettings, secondStepSplit, experimentDir, 2,
                    getFinalWeightsFileOfFirstStep(experimentSettings,experimentDir), featuresToOptimizeForInStep2,
                    experimentSettings.analysisSettings.copyToAnalysisDirWeightFiles);

            if (!experimentSettings.analysisSettings.copyToAnalysisDirWeightFiles) {

                // delete the weight files of the first step:

                //noinspection ConstantConditions
                for (int iteration=1; iteration<=experimentSettings.firstStepIterationsNum; iteration++)
                    try {
                        Files.deleteIfExists(experimentDir.getWeightsFile(1, iteration));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
            }

            csvRow = new ExperimentAnalysisCsvRow(experimentNum, experimentSettings, analysis1, analysis2);

        } else { // not doing 2-step training:
            SempreExperimentAnalysis analysis = runSempreExperiment(experimentSettings, split, experimentDir,
                    null, initialWeightsFile, null, experimentSettings.analysisSettings.copyToAnalysisDirWeightFiles);
            csvRow = new ExperimentAnalysisCsvRow(experimentNum, experimentSettings, analysis);
        }


        String gitCommitHashAfterRun = getHashOfHeadCommitIfLocalGitRepositoryDidNotChange();
        if (!Objects.equals(gitCommitHash, gitCommitHashAfterRun)){
            throw new RuntimeException(String.format(
                    "The app repository was changed during the experiment run. Commit before run: %s, Commit after: %s",
                    gitCommitHash, gitCommitHashAfterRun));
        }

        csvRow.addGitCommitHash(gitCommitHash);

        XStreamSerialization.writeObjectToXmlFile(csvRow, experimentDir.getAnalysisCsvRowXml());

        experimentDir.markAsDoneWithDoneIndicationFile();
    }

    /**
     * @param split the example split to use.
     *              This takes precedence over anything in 'settings', and any other argument of this method.
     * @param experimentDir the output sub-directory may already exist (due to first-step). But analysis subdirectories
 *                      must not already exist.
     * @param stepNumber null iff this experiment is not a two-step training experiment.
*                   Otherwise must be either 1 or 2.
     * @param initialWeightsFile the weights file to use as initial weights, or null if no initial weights file should
*                           be used.
*                           This takes precedence over anything in 'settings', and any other argument of this
*                           method.
     * @param featuresToOptimizeWeightsFor if null then all the weights are being optimized.
     * @param copyToAnalysisDirWeightFiles take precedence over 'settings'.
     */
    private SempreExperimentAnalysis runSempreExperiment(ExperimentSettings settings,
                                                         ExampleSplit split, ExperimentDirectory experimentDir,
                                                         @Nullable Integer stepNumber,
                                                         @Nullable Path initialWeightsFile,
                                                         @Nullable SafeSet<String> featuresToOptimizeWeightsFor,
                                                         boolean copyToAnalysisDirWeightFiles) {



        int iterationsNum = getIterationsNumberForSempreRun(settings, stepNumber);

        double regularizationCoefficient = settings.regularizationCoefficient;
        double initStepSize = settings.initStepSize;
        if (stepNumber != null && stepNumber == 1){
            if (settings.firstStepRegularizationCoefficient != null)
                regularizationCoefficient = settings.firstStepRegularizationCoefficient;
            if (settings.firstStepInitStepSize != null)
                initStepSize = settings.firstStepInitStepSize;
        }

        configSempreStaticOptions(settings, iterationsNum, initialWeightsFile, regularizationCoefficient, initStepSize);

        Verify.verify(currentRunningSempreExperiment == null);
        currentRunningSempreExperiment = new SempreExperiment(environmentSettings.moduleResourceDir,
                split, experimentDir.getAnalysisDirectory(stepNumber).toFile(), settings,
                stepNumber != null && stepNumber == 1,
                featuresToOptimizeWeightsFor);

        Builder builder = new Builder();
        builder.valueEvaluator = new InstructionValueEvaluator(); //This overrides the [-Builder.valueEvaluator] command line option.
        builder.buildUnspecified();
        currentRunningSempreExperiment.setSempreParser(builder.parser);
        SempreInstructionDataset sempreDataset = new SempreInstructionDataset(split);
        sempreDataset.read(); // depends on 'currentRunningSempreExperiment' being already set.
        Learner learner = new Learner(builder.parser, builder.params, sempreDataset);

        currentRunningSempreExperiment.analysis.reportSempreStarting();
        learner.learn();
        currentRunningSempreExperiment.analysis.reportSempreFinished();

        if (copyToAnalysisDirWeightFiles)
            moveWeightsFilesToAnalysisDirectory(experimentDir, stepNumber, iterationsNum,
                    settings.analysisSettings.copyToAnalysisDirNonFinalWeightFiles);

        SempreExperimentAnalysis result = currentRunningSempreExperiment.analysis;
        currentRunningSempreExperiment = null;
        return result;
    }

    /**
     * Moves weights files to experiment analysis directory.
     * Note: if a "bonus" iteration is done (i.e. one that does not update the weights), then there will be a
     * 'param.<Learner.opts.maxTrainIters>' file, but it would be identical to 'param.<Learner.opts.maxTrainIters-1>'.
     * @param iterationsNum the number of iterations that were done during execution.
     */
    private void moveWeightsFilesToAnalysisDirectory(ExperimentDirectory experimentDir, @Nullable Integer stepNumber,
                                                     int iterationsNum, boolean copyToAnalysisDirNonFinalWeightFiles) {
        int firstIterator = copyToAnalysisDirNonFinalWeightFiles ? 1 : iterationsNum;
        for (int i = firstIterator; i<=iterationsNum; i++){
            Path source = getSempreOutputDir().resolve("params." + (i-1));
            Path target = experimentDir.getWeightsFile(stepNumber, i);
            GeneralFileUtils.createDirectories(target.getParent());
            GeneralFileUtils.safeMove(source,target);
        }
    }

    /**
     * @param stepNumber null iff this experiment is not a two-step training experiment.
     *                   Otherwise must be either 1 or 2.
     */
    private int getIterationsNumberForSempreRun(ExperimentSettings settings, @Nullable Integer stepNumber) {
        int results;
        if (stepNumber==null || stepNumber.equals(2))
            results = settings.iterationsNum;
        else if (stepNumber.equals(1)) {
            assert settings.firstStepIterationsNum != null;
            results = settings.firstStepIterationsNum;
        }
        else
            throw new RuntimeException();
        return results;
    }


    /**
     * Returns an existing file or throws an exception.
     */
    private static Path getFinalWeightsFileOfFirstStep(ExperimentSettings settings,
                                                       ExperimentDirectory experimentDirectory) {

        assert settings.firstStepIterationsNum != null;
        // a later weights file shouldn't exist:
        Verify.verify(!Files.exists(experimentDirectory.getWeightsFile(1,settings.firstStepIterationsNum+1)));

        Path result = experimentDirectory.getWeightsFile(1, settings.firstStepIterationsNum);

        Verify.verify(Files.exists(result));
        return result;
    }



    private static Path getSempreOutputDir() {
        try {
            String outputDir = Execution.getFile("./");
            assert outputDir!=null;
            return new File(outputDir).getCanonicalFile().toPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * @param settings All other arguments take precedence over this one.
     * @param initialWeightsFile If this is null then no weights file is used (the relevant field in 'settings' is
     *                           ignored).
     */
    private static void  configSempreStaticOptions(ExperimentSettings settings, int iterationsNum,
                                                   @Nullable Path initialWeightsFile, double regularizationCoefficient,
                                                   double initStepSize) {


        // We use the same SemType for both the time-of-day and date time types.
        CanonicalNames.DATE = CanonicalNames.TIME;

        edu.stanford.nlp.sempre.Dataset.opts.maxExamples = new ArrayList<>();
        PhrasePredicateFeatureComputer.opts.lexicalizedPhrasePredicate = settings.useLexicalizedPhrasePredicateFeatures;
        PhrasePredicateFeatureComputer.opts.maxNforLexicalizeAllPairs = settings.maxNgramLengthForLexicalizedPhrasePredicateFeatures;
        Learner.opts.maxTrainIters = iterationsNum;
        Learner.opts.shuffleTrainingExamplesBetweenIterations = settings.shuffleTrainingExamplesBetweenIterations;

        if (initialWeightsFile != null) {
            // Sempre expects either an absolute path or a path relative to cwd.
            initialWeightsFile = initialWeightsFile.toAbsolutePath();
        }
        Builder.opts.inParamsPath = initialWeightsFile==null ? null : initialWeightsFile.toString();
        edu.stanford.nlp.sempre.Dataset.opts.splitRandom = new Random(1);
        Params.opts.l1RegCoeff = regularizationCoefficient;
        Params.opts.initRandom = new Random(1);
        Verify.verify(settings.initStepSize != 0);
        Params.opts.initStepSize = initStepSize;
        if (settings.lazyL1FullUpdateFreq < 0) {
            Params.opts.l1Reg = "nonlazy";
        } else {
            Verify.verify(settings.lazyL1FullUpdateFreq > 0);
            Params.opts.l1Reg = "lazy";
            Params.opts.lazyL1FullUpdateFreq = settings.lazyL1FullUpdateFreq;
        }
        Parser.opts.derivationScoreRandom = new Random(1);
        Parser.opts.beamSize = settings.beamSize;
        FloatingParser.opts.maxDepth = settings.maxDerivationSize;
        DPParser.opts.shuffleRandom = new Random(1);

        if (Params.opts.initWeightsRandomly || Params.opts.defaultWeight!=0)
            throw new RuntimeException("Bad sempre parameters: will cause features that are extracted only on test to have non-zero weight");
    }


    private static void verifyIgnoredSempreOptionsAreNotSet() {
        if(!edu.stanford.nlp.sempre.Dataset.opts.maxExamples.isEmpty())
            throw new RuntimeException("'Dataset.opts.maxExamples' must not be set");
        if(!Builder.opts.valueEvaluator.equals("__SET_IN_RUNTIME__"))
            throw new RuntimeException("'Builder.opts.valueEvaluator' must not be set");
        if (Builder.opts.inParamsPath != null)
            throw new RuntimeException("Builder.opts.inParamsPath must not be set");
        if (Learner.opts.maxTrainIters != 0)
            throw new RuntimeException("Learner.opts.maxTrainIters must not be set");
        if (Params.opts.l1RegCoeff != 0d)
            throw new RuntimeException("Params.opts.l1RegCoeff must not be set");
        if (!Params.opts.l1Reg.equals("none"))
            throw new RuntimeException("Params.opts.l1Reg must not be set to \"NA\"");
        if (Parser.opts.beamSize != 0)
            throw new RuntimeException("Parser.opts.beamSize must not be set");
        if (FloatingParser.opts.maxDepth != -1)
            throw new RuntimeException("FloatingParser.opts.maxDepth must not be set");

        // no way to request "empty" value for the following:
        if (!PhrasePredicateFeatureComputer.opts.lexicalizedPhrasePredicate)
            throw new RuntimeException("The value of -PhrasePredicateFeatureComputer.lexicalizedPhrasePredicate must not be set!");
        if (PhrasePredicateFeatureComputer.opts.maxNforLexicalizeAllPairs != -1)
            throw new RuntimeException("The value of -PhrasePredicateFeatureComputer.maxNforLexicalizeAllPairs must not be set!");
    }

    public static SempreExperiment getCurrentExperiment() {
        if (currentRunningSempreExperiment == null)
            throw new RuntimeException("no sempreExperiment is currently running");
        return currentRunningSempreExperiment;
    }


    public static boolean isExperimentCurrentlyRunning() {
        return currentRunningSempreExperiment!=null;
    }




    /**
     * @return null iff there were changes to the local repository relative to the head commit.
     */

    private String getHashOfHeadCommitIfLocalGitRepositoryDidNotChange() {


        String gitRepoRootPathStr = new CommandRunner().runAndBlock(true, false, null, null, "git", "rev-parse",
                "--show-toplevel").stdout.replaceAll("\\s+","");
        Path gitRepoRootPath = Paths.get(gitRepoRootPathStr);


        if (didGitRepositoryChangedRelativeToHeadCommit(gitRepoRootPath))
            return null;

        String cdCommand = getCommandToCdInto(gitRepoRootPath);
        return new CommandRunner().runAndBlock(true, true, true, null, cdCommand + " && git rev-parse HEAD").stdout;
    }

    private boolean didGitRepositoryChangedRelativeToHeadCommit(Path gitRepoRoot) {
        String cdCommand = getCommandToCdInto(gitRepoRoot);

        if (new CommandRunner().runAndBlock(false, true, true, null, cdCommand + " && git diff --exit-code").exitStatus != 0)
            return true; // unstaged changes.

        if (new CommandRunner().runAndBlock(false, true, true, null, cdCommand + "&& git diff --cached --exit-code").exitStatus
                != 0)
            return true; // staged changes.

        //noinspection RedundantIfStatement
        if (!new CommandRunner().runAndBlock(true, true, true, null, cdCommand +
                        "&& git ls-files --other --exclude-standard --no-empty-directory --directory")
                .stdout.isEmpty())
            return true; // new untracked files.

        return false;
    }


    private String getCommandToCdInto(Path path) {
        return "cd '" + path + "'";
    }

}
