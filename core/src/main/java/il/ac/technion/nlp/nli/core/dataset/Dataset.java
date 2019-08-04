package il.ac.technion.nlp.nli.core.dataset;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.io.GeneralFileUtils;
import com.ofergivoli.ojavalib.io.csv.CsvContent;
import il.ac.technion.nlp.nli.core.dataset.construction.ExampleCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents an entire dataset (containing examples from multiple domains, train-test split, etc.).
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class Dataset implements Serializable {


    private static final long serialVersionUID = 603127339885530111L;

	private final SafeMap<String, Example> exampleIdToExample = new SafeHashMap<>();
    /**
     * The order of examples is arbitrary (shuffling should be done before used as input for learning).
     */
    private ExampleSplit trainTestSplit = new ExampleSplit();
    private @NotNull DatasetDomains datasetDomains;
    private SafeMap<String, ExampleCategory> exampleIdToExampleCategory = new SafeHashMap<>();


    public Dataset(DatasetDomains datasetDomains) {
        this.datasetDomains = datasetDomains;
    }


    public Example getExampleById(String id) {
        return exampleIdToExample.safeGet(id);
    }


    public void addExample(Example ex){
        Verify.verify(exampleIdToExample.put(ex.getId(),ex) == null);
    }



    /**
     * Caller can modify.
     */
    public ExampleSplit getTrainTestSplit() {
        return trainTestSplit;
    }



    public Collection<Example> getExamples() {
        return exampleIdToExample.values();
    }


    public ExampleCategory getExampleCategory(Example ex) {
        return exampleIdToExampleCategory.safeGet(ex.getId());
    }


    public void setDatasetDomains(DatasetDomains datasetDomains) {
        this.datasetDomains = datasetDomains;
    }

    public DatasetDomains getDatasetDomains() {
        return datasetDomains;
    }


    public Collection<Example> getExamplesNotInTrainTestSplit() {
        return exampleIdToExample.values().stream()
                .filter(ex->!trainTestSplit.contains(ex))
                .collect(Collectors.toList());
    }


    public void setTrainTestSplit(ExampleSplit trainTestSplit) {
        this.trainTestSplit = trainTestSplit;
    }

    public SafeMap<String, Example> getExampleIdToExample() {
        return exampleIdToExample;
    }


    /**
     * Writes to a directory part of the data for each example (excluding the formal definition of the initial and
     * destination states).
     * @param outputDir May already exist. Missing directories along path are created.
     */
    public void writeHumanFriendlyRepresentationToDirectory(Path outputDir){
        GeneralFileUtils.createDirectories(outputDir);
        writeExamplesSummaryToCsv(trainTestSplit.getTrainExamples(), outputDir.resolve("train.csv"));
        writeExamplesSummaryToCsv(trainTestSplit.getTestExamples(), outputDir.resolve("test.csv"));
        writeExampleNumberSummary(outputDir.resolve("exampleNumberSummary.csv"));
    }

    private void writeExampleNumberSummary(Path outCsv) {
        CsvContent csv = new CsvContent("domain", "train", "test", "notInSplit");


        datasetDomains.getDomains().stream().sorted(Comparator.comparing(Domain::getId)).forEach(domain-> {

            Function<Collection<Example>,Long> countExamplesInDomain = examples->
                    examples.stream().filter(ex->ex.getDomain().equals(domain)).count();

            csv.addRow(domain.getId(),
                    "" + countExamplesInDomain.apply(getTrainTestSplit().getTrainExamples()),
                    "" + countExamplesInDomain.apply(getTrainTestSplit().getTestExamples()),
                    "" + countExamplesInDomain.apply(getExamplesNotInTrainTestSplit()));
        });
        csv.writeEntireCsv(outCsv);
    }

    private void writeExamplesSummaryToCsv(List<Example> examples, Path outputCsv) {
        CsvContent content = new CsvContent(Arrays.asList("example_id","domain" , "multiSentenceInstruction","nliMethod", "queryability","utterance"));
        examples.forEach(ex->content.addRow(Arrays.asList(
                ex.getId(), ex.getDomain().getId(),
                Boolean.toString(ex.isMultiSentenceInstruction()),
                getExampleCategory(ex).nliMethod.getName(),
                getExampleCategory(ex).instructionQueryability.toString(),
                ex.getInstructionUtterance())));
        content.writeEntireCsv(outputCsv);
    }

    private static String objectToString(@Nullable Object o) {
        if (o == null)
            return "";
        return o.toString();
    }


    /**
     * This modified the examples held by this data structure.
     */
    public void preprocessExamples(){

        exampleIdToExample.values().forEach(example->{
            String utterance = example.getInstructionUtterance();
            utterance = utterance.trim();

            // remove tailing dot.
            utterance = removeTailingDot(utterance);

            example.setInstructionUtterance(utterance);
        });
    }

    static String removeTailingDot(String utterance) {
        if(utterance.substring(utterance.length()-1,utterance.length()).equals("."))
            utterance = utterance.substring(0,utterance.length()-1);
        return utterance;
    }
}
