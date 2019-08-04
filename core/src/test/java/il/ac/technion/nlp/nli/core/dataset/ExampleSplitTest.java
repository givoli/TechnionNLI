package il.ac.technion.nlp.nli.core.dataset;

import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.core.dataset.construction.ExampleCategory;
import il.ac.technion.nlp.nli.core.dataset.construction.InstructionQueryability;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import org.junit.Test;

import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ExampleSplitTest {

    @Test
    public void createRandomSplitsForKFoldCrossValidationTest() {

        Random rand = new Random(0);

        for (int j=0; j<10; j++) {

            List<Example> examples = new LinkedList<>();

            int EXAMPLES_NUM = 100;
            int FOLDS_NUM = 4;
            assert EXAMPLES_NUM%FOLDS_NUM == 0;
            int WORKERS_NUM = 30;

            SafeMap<String, Integer> workerIdToExampleNum = new SafeHashMap<>();
            for (int i = 0; i<EXAMPLES_NUM; i++) {
                // we want exactly (desiredTestSetSize+1) examples from the same worker, and all others have unique workers.
                String workerId = Integer.toString(rand.nextInt(WORKERS_NUM));
                Example ex = new Example(Integer.toString(i), null, null, null, null);
                if (!workerIdToExampleNum.safeContainsKey(workerId))
                    workerIdToExampleNum.put(workerId,0);
                workerIdToExampleNum.put(workerId, workerIdToExampleNum.safeGet(workerId)+1);
                examples.add(ex);
            }
            int largestExampleNumDoneBySameWorker = Collections.max(workerIdToExampleNum.values());

            // verifying the splits we get are valid:

            ArrayList<ExampleSplit> splits = ExampleSplit.createRandomSplitsForKFoldCrossValidation(
                    examples, FOLDS_NUM, rand, false);
            aux_verifyKFoldSplitsAreValid(EXAMPLES_NUM, FOLDS_NUM, 0, splits);

            // worst case scenario: (FOLDS_NUM-1) subsets gets each (largestExampleNumDoneBySameWorker-1) more examples
            // than they should:
            int maxDifferenceBetweenActualAndDesiredExamplesNum = (FOLDS_NUM-1) * (largestExampleNumDoneBySameWorker-1);
            aux_verifyKFoldSplitsAreValid(EXAMPLES_NUM, FOLDS_NUM, maxDifferenceBetweenActualAndDesiredExamplesNum,
                    splits);


            // Making sure that each split is shuffled by demanding that no sequence of K-examples appears more than once
            // in all splits (in train and test lists)
            int K = 10;
            assert K < EXAMPLES_NUM / FOLDS_NUM;

            splits = ExampleSplit.createRandomSplitsForKFoldCrossValidation(
                    examples, FOLDS_NUM, rand, false);
            // each element is a sequence of k examples.
            SafeSet<List<Example>> k_sequences = new SafeHashSet<>();
            for (ExampleSplit split : splits) {
                aux_addAllKSequencesVerifyingNotAlreadyIn(split.getTrainExamples(), K, k_sequences);
                aux_addAllKSequencesVerifyingNotAlreadyIn(split.getTestExamples(), K, k_sequences);
            }
        }

    }



    /**
     * @param examples The example sequence from which we extract all k-sequences.
     */
    private void aux_addAllKSequencesVerifyingNotAlreadyIn(List<Example> examples, int k,
                                                           SafeSet<List<Example>> k_sequences) {
        ArrayList<Example> array = new ArrayList<>(examples);
        for (int i=0; i<array.size()-k+1; i++) {
            List<Example> currKSequence = new LinkedList<>();
            for (int j=0; j<k; j++)
                currKSequence.add(array.get(i+j));
            assertTrue(k_sequences.add(currKSequence));
        }
    }

    private void aux_verifyKFoldSplitsAreValid(int totalExamplesNum, int foldsNum,
                                               int maxDifferenceBetweenActualAndDesiredExamplesNum,
                                               ArrayList<ExampleSplit> splits) {
        assertEquals(foldsNum, splits.size());

        int desiredTestExamplesNum = totalExamplesNum / foldsNum;
        int desiredTrainExamplesNum = totalExamplesNum - desiredTestExamplesNum;
        int observedMaxDifferenceBetweenActualAndDesiredExamplesNum = 0;
        SafeSet<Example> examplesAppearingInTestSets = new SafeHashSet<>(); // should be all examples.
        for (ExampleSplit split : splits) {
            split.getTestExamples().forEach(ex -> assertTrue(examplesAppearingInTestSets.add(ex)));

            // asserting all examples are in the split:
            assertEquals(split.getTrainExamples().size(), totalExamplesNum - split.getTestExamples().size());

            observedMaxDifferenceBetweenActualAndDesiredExamplesNum = Math.max(observedMaxDifferenceBetweenActualAndDesiredExamplesNum,
                    split.getTrainExamples().size() - desiredTrainExamplesNum);

            observedMaxDifferenceBetweenActualAndDesiredExamplesNum = Math.max(observedMaxDifferenceBetweenActualAndDesiredExamplesNum,
                    split.getTestExamples().size() - desiredTestExamplesNum);
        }
        assertEquals(examplesAppearingInTestSets.size(), totalExamplesNum);
        assertTrue(observedMaxDifferenceBetweenActualAndDesiredExamplesNum <=
                        maxDifferenceBetweenActualAndDesiredExamplesNum);
    }


    @Test
    public void createRandomTrainDevSplitsForKFoldCrossValidationStratifiedOnExampleCategoryTest(){
        {

            Random rand = new Random(0);

            final ArrayList<ExampleCategory> CATEGORIES = new ArrayList<>(Arrays.asList(
                    new ExampleCategory(new MethodId(Object.class,  "a", Object.class), InstructionQueryability.OTHER),
                    new ExampleCategory(new MethodId(Object.class,  "a", List.class),   InstructionQueryability.OTHER),
                    new ExampleCategory(new MethodId(List.class,    "a", Object.class), InstructionQueryability.OTHER),
                    new ExampleCategory(new MethodId(Object.class,  "b", Object.class), InstructionQueryability.OTHER),
                    new ExampleCategory(new MethodId(Object.class,  "a", Object.class), InstructionQueryability.QUERYABLE_BY_PRIMITIVE_REL_AND_ARG_QUERYABLE_BY_SUPERLATIVE)
            ));


            // the category of an example is determined by its id (which is just a counter).
            // each category will have the same number of examples mapped to it.
            Function<Example, ExampleCategory> exampleToCategory = ex ->
                    CATEGORIES.get(Integer.parseInt(ex.getId()) % CATEGORIES.size());


            for (int j=0; j<10; j++) {

                List<Example> examples = new LinkedList<>();

                final int FOLDS_NUM = 4;
                final int EXAMPLES_NUM = CATEGORIES.size() * FOLDS_NUM * 10;
                final int EXAMPLES_NUM_PER_CATEGORY = EXAMPLES_NUM / CATEGORIES.size();

                for (int i = 0; i < EXAMPLES_NUM; i++)
                    examples.add(new Example(Integer.toString(i), null, null, null, null));

                ArrayList<ExampleSplit> splits =
                        ExampleSplit.createRandomTrainDevSplitsForKFoldCrossValidationStratifiedOnExampleCategory(
                                examples, FOLDS_NUM, exampleToCategory, rand, false);

                aux_verifyKFoldSplitsAreValid(EXAMPLES_NUM, FOLDS_NUM, 0, splits);

                // For each category make sure that the right number of examples of that category appear in each split
                // part of each split:
                for (ExampleCategory category : CATEGORIES) {
                    for (int foldInd = 0; foldInd < FOLDS_NUM; foldInd++) {
                        ExampleSplit s = splits.get(foldInd).getSubsetByFilteringExamples(null,
                                ex -> exampleToCategory.apply(ex).equals(category));
                        int examplesPerCategoryPerFold = EXAMPLES_NUM_PER_CATEGORY / FOLDS_NUM;
                        assertEquals(examplesPerCategoryPerFold, s.getTestExamples().size());
                        assertEquals(EXAMPLES_NUM_PER_CATEGORY-examplesPerCategoryPerFold,
                                s.getTrainExamples().size());
                    }
                }
            }
        }
    }

}