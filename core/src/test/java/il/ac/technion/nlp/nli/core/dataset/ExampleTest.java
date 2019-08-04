package il.ac.technion.nlp.nli.core.dataset;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ExampleTest {

    @Test
    public void isMultiSentenceInstructionTest() {

        List<String> multiSentenceInstructions = Arrays.asList(
                "a. a",
                "aaa. bbb.",
                "aaa. bbb. "
        );

        List<String> nonMultiSentenceInstructions = Arrays.asList(
                "",
                "a.",
                "a. ",
                "a b.   ",
                "aaa bbb ccc"
        );

        assertTrue(multiSentenceInstructions.stream().allMatch(str->
                new Example(null,str,null,null, null).isMultiSentenceInstruction()));

        assertTrue(nonMultiSentenceInstructions.stream().noneMatch(str->
                new Example(null,str,null,null, null).isMultiSentenceInstruction()));

    }
}