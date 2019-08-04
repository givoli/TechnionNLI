package il.ac.technion.nlp.nli.core.dataset;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.Maps;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import com.ofergivoli.ojavalib.io.log.Log;
import il.ac.technion.nlp.nli.core.dataset.construction.ExampleCategory;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A [train,test/dev] split.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ExampleSplit implements Serializable{

    /**
     * @param examples only examples from 'domain' are considered.
     * @return 'foldsNum' splits, each containing all examples in 'examples' that their domain is 'domain'.
     * The splits are the ones we get from K-fold cross validation stratified on example category.
     */
    public static List<ExampleSplit> getRandomSplitsForInDomainTrainingExperiment(
            List<Example> examples, Function<Example, ExampleCategory> exampleToCategory, Domain domain,
            Random rand, int foldsNum) {

        List<Example> domainExamples = examples.stream()
                .filter(ex -> ex.getDomain().equals(domain))
                .collect(Collectors.toList());

        ArrayList<ExampleSplit> result = ExampleSplit.createRandomTrainDevSplitsForKFoldCrossValidationStratifiedOnExampleCategory(domainExamples,
                foldsNum, exampleToCategory, rand, false);
        Verify.verify(result.size() == foldsNum);
        return result;
    }

    /**
     * @return a random (i.e. shuffled) split containing all examples in 'examples' s.t. the training examples are NOT
     * in 'testDomain' and the test examples are.
     */
    public static ExampleSplit getRandomSplitForZeroShotExperiment(List<Example> examples, Domain testDomain,
                                                                   Random rand) {

        List<Example> training = examples.stream()
                .filter(ex -> !ex.getDomain().equals(testDomain))
                .collect(Collectors.toList());

        List<Example> test = examples.stream()
                .filter(ex -> ex.getDomain().equals(testDomain))
                .collect(Collectors.toList());

        ExampleSplit result = new ExampleSplit(training, test);
        result.shuffleOrders(rand);
        return result;
    }

    public Stream<Example> getAllExamples() {
        return Stream.concat(trainExamples.stream(), testExamples.stream());
    }


    public enum SplitPart {
        TRAIN, TEST
    }

    private static final long serialVersionUID = -1344809441870724931L;


    private SafeSet<Example> trainExampleSet;
    private SafeSet<Example> testExampleSet;
    private ArrayList<Example> trainExamples;
    private ArrayList<Example> testExamples;

    public ExampleSplit() {
        this(new LinkedList<>(), new LinkedList<>());
    }

    /**
     * @param trainExamples copied (shallowly)
     * @param testExamples copied (shallowly)
     */
    public ExampleSplit(List<Example> trainExamples, List<Example> testExamples) {

        this.trainExamples = new ArrayList<>(trainExamples);
        this.testExamples = new ArrayList<>(testExamples);
        this.trainExampleSet = trainExamples.stream().collect(Collectors.toCollection(SafeHashSet::new));
        this.testExampleSet = testExamples.stream().collect(Collectors.toCollection(SafeHashSet::new));

        if (this.trainExamples.stream().anyMatch(ex->this.testExampleSet.safeContains(ex)))
            throw new RuntimeException("There's an example appearing both in train and test sets!");
    }


    public void shuffleOrders(Random rand) {
        Collections.shuffle(this.trainExamples, rand);
        Collections.shuffle(this.testExamples, rand);
    }


    /**
     * @param trainRatio the ratio between train examples # and total examples #, so it's in the range [0,1].
     *                   The test ratio will be 1-trainRatio.
     */
    public static ExampleSplit createRandomly(List<Example> examples, double trainRatio, Random rand,
                                               boolean sameWorkerExamplesStayTogether) {
        List<Double> ratios = Arrays.asList(trainRatio, 1-trainRatio);
        List<List<Example>> groups = randomlyShuffleAndCreateExamplePartition(examples, ratios ,rand,
                sameWorkerExamplesStayTogether);

        List<Example> train = groups.get(0);
        List<Example> test = groups.get(1);
        return new ExampleSplit(train, test); // no need to shuffle, orders are already random
    }

    /**
     * @param predicate examples that this predicates returns true for are kept.
     * @param splitPartToFilter if not null, only example in this part of the split are subject to being filtered out.
     * @return a subset of this split. The example collections are not shared with this object.
     */
    public ExampleSplit getSubsetByFilteringExamples(@Nullable SplitPart splitPartToFilter,
                                                     Predicate<Example> predicate) {

        Predicate<Example> completePredicate = ex-> (splitPartToFilter!=null &&  getSplitPart(ex) != splitPartToFilter) ||
                predicate.test(ex);

        List<Example> newTrainExampleOrder = trainExamples.stream()
                .filter(completePredicate)
                .collect(Collectors.toList());

        List<Example> newTestExampleOrder = testExamples.stream()
                .filter(completePredicate)
                .collect(Collectors.toList());

        return new ExampleSplit(newTrainExampleOrder, newTestExampleOrder);
    }


    /**
     * @return A subset of this split, containing only examples in 'c' (their order is preserved).
     */
    public ExampleSplit getSubsetContainedInExampleCollection(Collection<Example> c) {
        SafeSet<Example> set = c.stream().collect(Collectors.toCollection(SafeHashSet::new));
        return getSubsetByFilteringExamples(null, set::safeContains);
    }

    /**
     * @return A subset of this split, containing only examples not in 'c' (their order is preserved).
     */
    public ExampleSplit getSubsetNotContainedInExampleCollection(Collection<Example> c) {
        SafeSet<Example> set = c.stream().collect(Collectors.toCollection(SafeHashSet::new));
        return getSubsetByFilteringExamples(null, ex->!set.safeContains(ex));
    }

    /**
     * @return The example collections are not shared with this object.
     */
    public ExampleSplit getSubsetNotContainingMultiSentenceExamples() {
        return getSubsetByFilteringExamples(null, ex->!ex.isMultiSentenceInstruction());
    }

    public ExampleSplit getSubsetContainingSingleExample(String exId) {
        return getSubsetByFilteringExamples(null, ex->ex.getId().equals(exId));
    }


    /**
     * Input splits are not modified.
     * @param rand if null then no shuffling occurs and the order is preserved: 'split1' and then 'split2'.
     *             Otherwise, the orders are shuffled using 'rand'.
     */
    public static ExampleSplit addSplits(ExampleSplit split1, ExampleSplit split2, @Nullable Random rand) {

        List<Example> newTrainExampleOrder = new ArrayList<>();
        newTrainExampleOrder.addAll(split1.getTrainExamples());
        newTrainExampleOrder.addAll(split2.getTrainExamples());

        List<Example> newTestExampleOrder = new ArrayList<>();
        newTestExampleOrder.addAll(split1.getTestExamples());
        newTestExampleOrder.addAll(split2.getTestExamples());

        ExampleSplit result = new ExampleSplit(newTrainExampleOrder, newTestExampleOrder);
        if (rand != null)
            result.shuffleOrders(rand);
        return result;
    }


    /**
     * @param examples unchanged.
     * @param proportionalSubsetSizes The ratio between elements of this vector define the desired ratio between the
     *                                respective subsets. There's no restriction on the elements sum.
     * @param rand source of randomness.
     * @param sameWorkerExamplesStayTogether when true, examples from the same worker will be together in the same
     *                                       group of the returned partition.
     *                                       WARNING: when this is true you might get subsets that differentiate
     *                                       substantially from the desired sizes (in cases where a subset construction
     *                                       is almost done and then a huge bulk of examples of the same worker are
     *                                       added to it).
     * @return a random partition of 'examples'. The number of subsets returned is the size of proportionalSubsetSizes.
     * The returned lists are a random splitting of 'examples', and their order is random.
     * In order to enforce the chosen settings, the sizes of the returned lists might be slightly off (in regard to
     * proportionalSubsetSizes): some (perhaps empty) prefix of the returned lists will have more examples than
     * asked for, and each of the rest might have less than asked for.
     * The implementation shuffles, adds examples to each subset in turn until it's big enough, and then shuffles each
     * subset.
     */
    private static List<List<Example>> randomlyShuffleAndCreateExamplePartition(
            List<Example> examples, List<Double> proportionalSubsetSizes, Random rand,
            boolean sameWorkerExamplesStayTogether) {

        /*
         * Each group of examples will belong to a single part of the split.
         */
        ArrayList<ArrayList<Example>> examplesGroups;

        if (sameWorkerExamplesStayTogether) {
            throw new RuntimeException("not currently implemented");
        } else {
            // Each example is in its own group.
            examplesGroups = examples.stream()
                    .map(ExampleSplit::wrapWithArrayList)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        Collections.shuffle(examplesGroups, rand);
        examplesGroups.forEach(list->Collections.shuffle(list,rand));

        // Now we simply walk linearly on examplesGroups and fill up the returned lists one by one.
        double proportionalSizesSum = proportionalSubsetSizes.stream().reduce(0.0, Double::sum);
        List<List<Example>> result = new LinkedList<>();

        int nextIndInShuffledList = 0;
        for (Double proportionalSize : proportionalSubsetSizes) {
            ArrayList<Example> currentExamples = new ArrayList<>();
            result.add(currentExamples);
            int wantedSizeOfCurr = (int) Math.round(proportionalSize / proportionalSizesSum * examples.size());
            while (currentExamples.size() < wantedSizeOfCurr && nextIndInShuffledList < examplesGroups.size()) {
                currentExamples.addAll(examplesGroups.get(nextIndInShuffledList));
                nextIndInShuffledList++;
            }
            if (currentExamples.isEmpty())
                Log.warn("Too few examples! An empty partition remained.");
        }
        // Now appending remaining example groups.
        int indexOfListToAppendTo = 0;
        while (nextIndInShuffledList < examplesGroups.size()){
            result.get(indexOfListToAppendTo).addAll(examplesGroups.get(nextIndInShuffledList));
            indexOfListToAppendTo++;
            nextIndInShuffledList++;
        }
        for (List<Example> list : result){
            Collections.shuffle(list, rand); // we don't want any dependency of the order on group sizes.
        }
        Verify.verify(nextIndInShuffledList==examplesGroups.size()); // otherwise there are unused examples.
        Verify.verify(result.stream().map(List::size).reduce(0, Integer::sum) == examples.size());
        return result;

    }

    private static ArrayList<Example> wrapWithArrayList(Example ex) {
        ArrayList<Example> l = new ArrayList<>();
        l.add(ex);
        return l;
    }

    /**
     * @return true iff ex is either in the train or test sets.
     */
    public boolean contains(Example ex) {
        return trainExampleSet.safeContains(ex) || testExampleSet.safeContains(ex);
    }

    public ArrayList<Example> getTrainExamples() {
        return trainExamples;
    }

    public ArrayList<Example> getTestExamples() {
        return testExamples;
    }


    /**
     * @return null iff 'ex' is not in either part of this split.
     */
    public @Nullable SplitPart getSplitPart(Example ex) {
        if (trainExampleSet.safeContains(ex)) {
            Verify.verify(!testExampleSet.safeContains(ex));
            return SplitPart.TRAIN;
        }
        if (testExampleSet.safeContains(ex)) {
            Verify.verify(!trainExampleSet.safeContains(ex));
            return SplitPart.TEST;
        }
        return null;
    }

    @Override
    public String toString() {
        return "[" + trainExamples.size() + ":" + testExamples.size() + "]";
    }


    /**
     * See: {@link #randomlyShuffleAndCreateExamplePartition(List, List, Random, boolean)} for details.
     * @return Each returned split represents a different fold in the k-fold cross validation settings.
     */
    static ArrayList<ExampleSplit> createRandomSplitsForKFoldCrossValidation(
            List<Example> examples, int foldsNum, Random rand, boolean sameWorkerExamplesStayTogether) {

        List<Double> proportionSubsetSizes = Collections.nCopies(foldsNum, 1.0/foldsNum);
        List<List<Example>> partition = randomlyShuffleAndCreateExamplePartition(examples, proportionSubsetSizes, rand,
                sameWorkerExamplesStayTogether);

        ArrayList<ExampleSplit> result = new ArrayList<>(foldsNum);
        for (int subsetUsedAsTestInd=0; subsetUsedAsTestInd<foldsNum; subsetUsedAsTestInd++) {
            List<Example> foldTestExamples = partition.get(subsetUsedAsTestInd);
            List<Example> foldTrainExamples = new LinkedList<>();
            for (int i=0; i<foldsNum; i++)
                if (i != subsetUsedAsTestInd)
                    foldTrainExamples.addAll(partition.get(i));
            ExampleSplit split = new ExampleSplit(foldTrainExamples, foldTestExamples);
            split.shuffleOrders(rand);
            Verify.verify(split.getAllExamples().count() == examples.size());
            result.add(split);
        }
        return result;
    }

    /**
     * The random separation to (almost) equal-sized folds is done per {@link ExampleCategory}.
     * @param examples the examples to be used for each of the returned splits.
     * @return random train-dev folds. Each fold is a random split (also randomly shuffled) containing all examples in
     * 'examples').
     */
    public static ArrayList<ExampleSplit> createRandomTrainDevSplitsForKFoldCrossValidationStratifiedOnExampleCategory(
            List<Example> examples, int foldsNum, Function<Example, ExampleCategory> exampleToCategory,
            Random rand, boolean sameWorkerExamplesStayTogether) {

        SafeMap<ExampleCategory, List<Example>> categoryToExamples = new SafeHashMap<>();
        /**
         * The following stack is used only to have some (arbitrary) deterministic order over the categories.
         * Iterating directly over the pairs of categoryToExamples is bad because the order of the pairs is not
         * deterministic (e.g. might depend on whether you run in debug configuration or not).
         */
        Stack<ExampleCategory> deterministicCategoryOrder = new Stack<>();
        examples.forEach(ex-> {
            ExampleCategory category = exampleToCategory.apply(ex);
            Maps.addToMapOfCollections(categoryToExamples, category, ex, LinkedList::new);
            deterministicCategoryOrder.add(category);
        });

        ArrayList<ExampleSplit> result = new ArrayList<>();
        for (int i=0;i<foldsNum;i++)
            result.add(new ExampleSplit());

        while (!categoryToExamples.isEmpty()) {
            ExampleCategory category = deterministicCategoryOrder.pop();
            if (!categoryToExamples.safeContainsKey(category))
                continue;
            List<Example> examplesOfCategory = categoryToExamples.safeGet(category);
            categoryToExamples.safeRemove(category);

            ArrayList<ExampleSplit> splitsForCurrentCategory = createRandomSplitsForKFoldCrossValidation(
                    examplesOfCategory, foldsNum, rand, sameWorkerExamplesStayTogether);
            for (int i=0;i<foldsNum;i++)
                result.set(i,  addSplits(result.get(i), splitsForCurrentCategory.get(i), null));

        }
        for (int i=0;i<foldsNum;i++)
            result.get(i).shuffleOrders(rand);
        return result;
    }

}
