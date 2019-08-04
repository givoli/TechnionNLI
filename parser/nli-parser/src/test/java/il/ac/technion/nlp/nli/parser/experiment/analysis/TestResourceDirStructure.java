package il.ac.technion.nlp.nli.parser.experiment.analysis;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class TestResourceDirStructure {


    public static Path getFeatureGeneralityScoresDir() {
        return Paths.get("src/test/resources/featureGeneralityScores");
    }
}
