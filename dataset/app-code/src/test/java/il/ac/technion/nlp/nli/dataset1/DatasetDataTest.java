package il.ac.technion.nlp.nli.dataset1;

import com.google.common.base.Verify;
import ofergivoli.olib.io.serialization.SerializationUtils;
import ofergivoli.olib.io.serialization.xml.XStreamSerialization;
import il.ac.technion.nlp.nli.core.dataset.Dataset;
import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Tests the validity of the dataset (and not the logic of the actual {@link Dataset} class).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class DatasetDataTest {


    /**
     * The file type can be either ser or xml.
     */
    private static final String relativePathOfDataset = "../data/EntireDataset.xml";



    public static Dataset readOnlyDatasetToTestOn = readDataset();

    private static Dataset readDataset() {
        Path path = Paths.get(relativePathOfDataset);
        if(!Files.exists(path))
            throw new RuntimeException("Missing the following dataset file to test on: " + path);

        if (path.toString().endsWith(".ser"))
            return SerializationUtils.readObjectFromFile(path.toFile());
        if (path.toString().endsWith(".xml"))
            return XStreamSerialization.readObjectFromTrustedXmlFile(false, path);
        throw new RuntimeException("invalid dataset file type");
    }


    @Test
    public void trainTestSplitIsValid() {
        ExampleSplit trainTestSplit = readOnlyDatasetToTestOn.getTrainTestSplit();
        // asserting that no example appears both in train and test sets:
        assertTrue(trainTestSplit.getTrainExamples().stream()
                .noneMatch(ex->trainTestSplit.getSplitPart(ex) == ExampleSplit.SplitPart.TEST));
    }

    @Test
    public void makeSureAllInstructionUtterancesAreReasonable() {
        readOnlyDatasetToTestOn.getExamples().forEach(ex->{
            if(!isInstructionUtteranceReasonable(ex.getInstructionUtterance()))
                throw new RuntimeException("Example " + ex.getId() + " has an unreasonable utterance:\n"
                        + ex.getInstructionUtterance());
        });
    }

    @Test
    public void testStateEquality() {
        Verify.verify(!readOnlyDatasetToTestOn.getExamples().isEmpty());

        //Sanity check: every state should be identical to itself:
        readOnlyDatasetToTestOn.getExamples().forEach(ex-> {
            if (!ex.getInitialState().entityGraphsEqual(ex.getInitialState()))
                throw new RuntimeException("");
        });

        // The initial and desired states should be different:
        readOnlyDatasetToTestOn.getExamples().forEach(ex-> {
            if (ex.getInitialState().entityGraphsEqual(ex.getDestinationState()))
                throw new RuntimeException("Initial and desired states are identical!");
        });
    }

    /**
     * Checking words number is not below a given threshold.
     * This verifies you don't have utterances which are "#NAME?" - coming from Excel interpreting an utterance as a
     * formula (because it starts with "-").
     */
    private boolean isInstructionUtteranceReasonable(String instruction) {
        int MIN_WORDS_NUM_IN_REASONABLE_UTTERANCE = 2;
        return instruction.trim().split("\\s+").length >= MIN_WORDS_NUM_IN_REASONABLE_UTTERANCE;
    }

}