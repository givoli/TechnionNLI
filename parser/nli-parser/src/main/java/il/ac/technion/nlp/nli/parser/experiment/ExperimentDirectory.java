package il.ac.technion.nlp.nli.parser.experiment;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.io.GeneralFileUtils;
import com.ofergivoli.ojavalib.io.files_tree.FileTreeManager;
import com.ofergivoli.ojavalib.io.serialization.xml.XStreamSerialization;
import il.ac.technion.nlp.nli.parser.experiment.analysis.results.ExperimentAnalysisCsvRow;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines the structure of an experiment directory.
 * An experiment directory contains files defining the settings of the experiment, and optionally files with the
 * results/analysis of running the experiment.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ExperimentDirectory {

    public Path getOutputDirectory() {
        return experimentDirPath.resolve("output");
    }

    /**
     * @return the file exists if and only if the output is complete, i.e. the experiment execution was completed
     * successfully (and the output copied successfully from remote if relevant).
     */
    public Path getDoneIndicationFile() {
        return getOutputDirectory().resolve("done");
    }

    private Path getExperimentSettingsXml(){
        return experimentDirPath.resolve(ExperimentSettings.class.getSimpleName()+ ".xml");
    }



    public Path getAnalysisCsvRowXml(){
        /**
         * The file is not inside the analysis directory, because we don't want to zip it.
         */
        return getOutputDirectory().resolve(ExperimentAnalysisCsvRow.class.getSimpleName() + ".xml");
    }

    public Path getStderrPath() {
        return getOutputDirectory().resolve("stderr.txt");
    }


    /**
     * Exception is thrown if there's a non-experiment directory in t'experimentsParentDirectory'.
     * @return sorted by experiment ids.
     */
    public static List<ExperimentDirectory> getAllExperimentDirectoriesInDirectory(Path experimentsParentDirectory) {
        List<ExperimentDirectory> result = FileTreeManager.getFilesInDirectory(experimentsParentDirectory).stream()
                .filter(f -> f.toFile().isDirectory())
                .map(ExperimentDirectory::new)
                .sorted((a,b)-> compareExperimentIds(a.getExperimentId(),b.getExperimentId()))
                .collect(Collectors.toList());
        Verify.verify(result.stream().allMatch(experimentDirectory->isExperimentDirectory(
                experimentDirectory.experimentDirPath)));

        return result;
    }

    /**
     * @param experimentsRootDirectory experimentsRootDirectory may contain other unrelated files and directories under
     *                                 it.
     * @return sorted by experiment ids.
     */
    public static List<ExperimentDirectory> getAllExperimentDirectoriesUnderDirectory(Path experimentsRootDirectory) {
        List<ExperimentDirectory> result = FileTreeManager.getAllDirectoriesUnderDir(experimentsRootDirectory).stream()
                .filter(ExperimentDirectory::isExperimentDirectory)
                .map(ExperimentDirectory::new)
                .sorted((a,b)-> compareExperimentIds(a.getExperimentId(),b.getExperimentId()))
                .collect(Collectors.toList());
        return result;
    }

    private static boolean isExperimentDirectory(Path path) {
        return Files.isDirectory(path) &&
                Files.isRegularFile(new ExperimentDirectory(path).getExperimentSettingsXml());
    }

    /**
     * If 'a' and 'b' are integers than we compare as integers, otherwise we compare as strings.
     */
    public static int compareExperimentIds(String a, String b) {
        try {
            Integer ai = Integer.parseInt(a);
            Integer bi = Integer.parseInt(b);
            return ai.compareTo(bi);
        } catch(NumberFormatException e) {
            return a.compareTo(b);
        }
    }


    private final Path experimentDirPath;

    public ExperimentDirectory(Path experimentDirectory) {
        this.experimentDirPath = experimentDirectory;
    }







    /**
     * @param stepNumber must be null iff the experiment is not 2-step. Otherwise must be either 1 or 2. The returns
     *                   path is the same for both null and '2'.
     */
    public Path getAnalysisDirectory(@Nullable Integer stepNumber){
        if (stepNumber == null || stepNumber == 2)
            return getOutputDirectory().resolve("analysis");
        else if (stepNumber == 1)
            return getOutputDirectory().resolve("analysis/step1");
        throw new RuntimeException();
    }

    /**
     * Returns the path of the weights file that should contain weights we get at the end of the given
     * iteration.
     * @param stepNumber must be null iff the experiment is not 2-step. Otherwise must be either 1 or 2.
     * @param iteration one-based.
     */
    public Path getWeightsFile(@Nullable Integer stepNumber, int iteration){
//        return getAnalysisDirectory(stepNumber).resolve("weightsAtEndOfIter" + iteration); TODO: change to this.
        return getAnalysisDirectory(stepNumber).resolve("params." + (iteration-1));
    }

    public String getExperimentId() {
        return experimentDirPath.getFileName().toString();
    }


    public Path getPath() {
        return experimentDirPath;
    }


    public void deleteOutputDirectoryIfExists() {
        if (!Files.exists(getOutputDirectory()))
            return;
        try {
            deleteOutputDirectory();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void deleteOutputDirectory() throws IOException {
        File outputDir = getOutputDirectory().toFile();
        FileUtils.deleteDirectory(outputDir);
    }

    public boolean isDone() {
        return Files.exists(getDoneIndicationFile());
    }

    public void markAsDoneWithDoneIndicationFile() {
        try {
            //noinspection EmptyTryBlock
            try (FileOutputStream ignored = new FileOutputStream(getDoneIndicationFile().toFile())) {
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }


    /**
     * This method should be invoked after this experiment directory was already executed.
     * This method creates a new evaluation-only experiment directory (i.e. one for an experiment in which no training
     * takes place), using as final weights that were already learned in this experiment.
     *
     * @param idsOfExamplesToEvaluateOn if null then the test examples of the original experiment are used.
     * @param trainingIterations the number of training iterations that the weights used are the result of.
     *                           If null then the total number iterations in the original experiment is used.
     * @param outputExperimentDir must not already exist. Directory along the path may be missing.
     */
    public void createEvaluationOnlyVersion(@Nullable List<String> idsOfExamplesToEvaluateOn,
                                            @Nullable Integer trainingIterations,
                                            boolean ignoreUnknownElementsInExperimentSettingsXml,
                                            Path outputExperimentDir){
        Verify.verify(isDone());

        ExperimentSettings experimentSettings = readExperimentSettings(ignoreUnknownElementsInExperimentSettingsXml);

        if (trainingIterations == null)
            trainingIterations = experimentSettings.iterationsNum;
        if (idsOfExamplesToEvaluateOn == null)
            idsOfExamplesToEvaluateOn = experimentSettings.testExampleIds;


        Path weightsPathRelativeToExperimentDir = Paths.get("initialWeights.weights");
        ExperimentSettings evalOnlySettings =
                experimentSettings.getEvaluationOnlyVersion(idsOfExamplesToEvaluateOn,
                        weightsPathRelativeToExperimentDir);


        ExperimentDirectory outputDir = new ExperimentDirectory(outputExperimentDir);
        outputDir.writeDirectory(evalOnlySettings);

        GeneralFileUtils.copy(
                getWeightsFile(experimentSettings.twoStepTraining ? 2 : null, trainingIterations),
                outputDir.getPath().resolve(weightsPathRelativeToExperimentDir));
    }

    /**
     * The experiment directory must not already exist. Other directories along its path may be missing.
     */
    private void writeDirectory(ExperimentSettings settings) {
        Verify.verify(!Files.exists(getPath()));
        GeneralFileUtils.createDirectories(getPath());
        writeExperimentSettings(settings);
    }

    /**
     * @param experimentSettings if null, we read the experiment settings from file.
     */
    public Path getFinalWeightsLearned(@Nullable ExperimentSettings experimentSettings) {
        if (experimentSettings==null)
            experimentSettings = readExperimentSettings(false);
        if (experimentSettings.twoStepTraining)
            return getWeightsFile(2, experimentSettings.iterationsNum);
        return getWeightsFile(null, experimentSettings.iterationsNum);
    }

    /**
     * Write experiment settings, overwriting existing settings.
     */
    public void writeExperimentSettings(ExperimentSettings experimentSettings) {
        XStreamSerialization.writeObjectToXmlFile(experimentSettings, getExperimentSettingsXml());
    }

    @Override
    public String toString() {
        return experimentDirPath.toString();
    }

    public ExperimentSettings readExperimentSettings(boolean ignoreUnknownElementsInXml) {
        ExperimentSettings result = XStreamSerialization.readObjectFromTrustedXmlFile(ignoreUnknownElementsInXml,
                getExperimentSettingsXml().toFile());

        if (result.enableInstructionFeatures == null)
            result.enableInstructionFeatures = true;

        return result;
    }
}
