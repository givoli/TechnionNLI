package ofergivoli.olib.data_structures.tree;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Generates random trees containing random (non-negative) int values.
 */
public class RandomIntTreeGenerator {

    private RandomTreeGenerator<Integer, TreeNode<Integer>> randomTreeGenerator;
    private Random rand;

    private static final int MAX_INT_VAL = 1000;

    /**
     * @param rand source of randomness.
     */
    public RandomIntTreeGenerator(Random rand) {

        this.rand = rand;
        randomTreeGenerator = new RandomTreeGenerator<>(rand,
                (integer, childNodes) -> new TreeNode<>(integer,childNodes));
    }


    /**
     * Generated a tree with random structure and node values.
     * @param maxChildNum The maximum number of child nodes a node can have.
     */
    public Tree<Integer,TreeNode<Integer>> generateRandomTree(int nodesNum, int maxChildNum) {

        List<Integer> nodeValues = new LinkedList<>();
        for (int i=0; i<nodesNum; i++)
            nodeValues.add(rand.nextInt(MAX_INT_VAL));
        return randomTreeGenerator.generateTreeWithRandomStructure(nodeValues,maxChildNum);
    }

}
