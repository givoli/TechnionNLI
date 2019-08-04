package il.ac.technion.nlp.nli.parser.experiment;

import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.io.GeneralFileUtils;
import com.ofergivoli.ojavalib.io.files_tree.FileTreeManager;
import com.ofergivoli.ojavalib.io.serialization.xml.XStreamSerialization;
import il.ac.technion.nlp.nli.parser.experiment.analysis.results.ExperimentAnalysisCsvRow;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ExperimentBatch implements Serializable{
    private static final long serialVersionUID = 6642440624914572020L;

    private int nextExperimentNumToAssign = 1;


    /**
     * The experiment id should be a valid filename.
     */
    private final SafeMap<String,ExperimentSettings> experimentIdToExperimentSettings = new SafeHashMap<>();

    public ExperimentBatch() {
    }

    public ExperimentBatch(List<ExperimentSettings> settings, int experimentNumberToStartFrom) {
        nextExperimentNumToAssign = experimentNumberToStartFrom;
        settings.forEach(this::add);
    }


    public void add(ExperimentSettings settings){
        int expNum = nextExperimentNumToAssign++;
        experimentIdToExperimentSettings.put(String.valueOf(expNum), settings);
    }


    /**
     * @param batchRootDirectory must not already exist.
     */
    public void createFileTree(Path batchRootDirectory){
        if (Files.exists(batchRootDirectory))
            throw new RuntimeException("batch directory already exists: " + batchRootDirectory);

        GeneralFileUtils.createDirectory(batchRootDirectory);

        experimentIdToExperimentSettings.forEach((experimentId, settings)-> {
            Path experimentDirectoryPath = getExperimentDirectoryPath(batchRootDirectory, experimentId);
            GeneralFileUtils.createDirectory(experimentDirectoryPath);
                    new ExperimentDirectory(experimentDirectoryPath).writeExperimentSettings(settings);
        });
    }


    private static Path getExperimentDirectoryPath(Path batchRootDir, String experimentId) {
        return batchRootDir.resolve(experimentId);
    }



    /**
     * @param batchDir the directory containing the experiment directories.
     *                     The output csv will be created in this directory.
     * @param acceptIncompleteExperiments when false, unfinished experiments will cause exception throw.
     *                                    Note: in case true, the incomplete experiments are ignored and are not
     *                                    included in the csv file.
     */
    public static void writeAllAnalysisCsvRowsIntoSingleCsv(Path batchDir, boolean acceptIncompleteExperiments){

        List<ExperimentAnalysisCsvRow> csvRows = new ArrayList<>();


        List<ExperimentDirectory> experimentDirectories = FileTreeManager.getFilesInDirectory(batchDir).stream()
                .filter(Files::isDirectory)
                .map(ExperimentDirectory::new)
                .sorted(Comparator.comparing(ExperimentDirectory::getExperimentId))
                .collect(Collectors.toList());


        boolean allExperimentsCompleted = experimentDirectories.stream().allMatch(ExperimentDirectory::isDone);

        if (!acceptIncompleteExperiments && !allExperimentsCompleted)
            throw new RuntimeException("not all experiments completed");


        experimentDirectories.forEach(experimentDirectory -> {
                    Path csvRowXmlFile = experimentDirectory.getAnalysisCsvRowXml();
                    if (!Files.exists(csvRowXmlFile) && acceptIncompleteExperiments) {
                        return;
                    }
                    csvRows.add(XStreamSerialization.readObjectFromTrustedXmlFile(false, csvRowXmlFile.toFile()));
                });
        String outputFilename = allExperimentsCompleted ? "analysis.csv" : "partial_analysis.csv";
        ExperimentAnalysisCsvRow.getAsCsvContent(csvRows).writeEntireCsv(batchDir.resolve(outputFilename));
    }


    public Collection<ExperimentSettings> getExperiments() {
        return experimentIdToExperimentSettings.values();
    }
}
