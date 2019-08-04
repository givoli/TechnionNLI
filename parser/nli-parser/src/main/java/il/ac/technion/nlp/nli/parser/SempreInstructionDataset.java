package il.ac.technion.nlp.nli.parser;

import com.ofergivoli.ojavalib.io.log.Log;
import edu.stanford.nlp.sempre.ContextValue;
import edu.stanford.nlp.sempre.Dataset;
import edu.stanford.nlp.sempre.Example.Builder;
import edu.stanford.nlp.sempre.LanguageInfo;
import edu.stanford.nlp.sempre.ListValue;
import fig.basic.LogInfo;
import il.ac.technion.nlp.nli.core.dataset.Example;
import il.ac.technion.nlp.nli.core.dataset.ExampleSplit;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.denotation.ExplicitStateValue;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;


/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class SempreInstructionDataset extends Dataset {

    private final ExampleSplit trainTestSplit;

    public SempreInstructionDataset(ExampleSplit trainTestSplit) {
        this.trainTestSplit = trainTestSplit;
    }

    /**
     * Dirty hack: in this overloaded method the semantics of the first argument changed from 'path' to 'groupId'.
     *
     * Inserts elements to 'outExample' from {@link #trainTestSplit} (either from train or test/dev split).
     * @param groupId should be either "train", "dev" or "test" (last two have identical effect).
     */
    @Override
    protected void readLispTreeHelper(String groupId, int maxExamples,
                                      List<edu.stanford.nlp.sempre.Example> outExamples) {

        if (outExamples.size() >= maxExamples) return;
        LogInfo.begin_track("Reading %s", groupId);

        List<Example> examples;
        switch (groupId) {
            case "train":
                examples = trainTestSplit.getTrainExamples();
                break;
            case "dev":
            case "test":
                examples = trainTestSplit.getTestExamples();
                break;
            default:
                throw new RuntimeException("invalid 'groupId'");
        }

        Iterator<Example> exIt = examples.iterator();
        while (outExamples.size() < maxExamples && exIt.hasNext()) {

            Example instructionEx = exIt.next();
            edu.stanford.nlp.sempre.Example sempreEx = createSempreExample(instructionEx);
            sempreEx.preprocess();

            // Skip example if too long
            if (sempreEx.numTokens() > opts.maxTokens) {
                Log.error("sempreEx.numTokens() > opts.maxTokens");
                continue;
            }

            LogInfo.logs("Example %s (%d): %s => %s", sempreEx.id, outExamples.size(), sempreEx.getTokens(), sempreEx.targetValue);

            outExamples.add(sempreEx);
            numTokensFig.add(sempreEx.numTokens());
            tokenTypes.addAll(sempreEx.getTokens());

        }
        LogInfo.end_track();

    }

    private edu.stanford.nlp.sempre.Example createSempreExample(Example example) {
        Builder builder = new Builder();

        builder.setId(example.getId());
        builder.setUtterance(example.getInstructionUtterance());

        ExplicitStateValue destinationStateValue = new ExplicitStateValue(example.getDestinationState());
        builder.setTargetValue(new ListValue(Collections.singletonList(destinationStateValue)));
        builder.setContext(new ContextValue(new InstructionKnowledgeGraph(example.getInitialState(),
                ExperimentRunner.getCurrentExperiment().settings.deterministic)));
        builder.setLanguageInfo(new LanguageInfo());

        return builder.createExample();
    }

}
