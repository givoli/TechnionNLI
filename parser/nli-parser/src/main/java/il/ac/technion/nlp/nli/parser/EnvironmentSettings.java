package il.ac.technion.nlp.nli.parser;

import il.ac.technion.nlp.nli.parser.experiment.ExperimentSettings;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Settings that their value either is not directly read by the parser, or depends on absolute file paths; and thus
 * shouldn't be at {@link ExperimentSettings}.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class EnvironmentSettings {

    public final Path moduleResourceDir;
    public final Path datasetSer;


    public Path tempSempreDirectoriesParentRelativeToHomeDir = Paths.get("tmp/TempSempreDirectories");

    /**
     * The size of the memory allocation pool for the JVM running the parsing algorithm.
     */
    public int memoryAllocationPoolInMiB = 1500;


    // Fields defining the structure of the resource directory:
    public Path sempreDataDirRelativeToResourceDir = Paths.get("SempreData");
    public Path sempreArgsXmlFileRelativeToSempreDataDir = Paths.get("SempreDefaultArgs.xml");



    public EnvironmentSettings(Path moduleResourceDir, Path datasetSer) {
        this.moduleResourceDir = moduleResourceDir;
        this.datasetSer = datasetSer;
    }

}
