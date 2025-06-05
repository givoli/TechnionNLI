package ofergivoli.olib.data_structures.tree;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

/**
 * @param <T> The data type in each node of the generated trees.
 * @param <Node> The node type in teh generated trees.
 *
 */
public class RandomTreeGenerator<T, Node extends TreeNode<T>> {

    private final Random rand;
    private final BiFunction<T, List<Node>, Node> nodeGenerator;

    /**
     * @param rand Source of randomness.
     * @param nodeGenerator Generated a node given its data and its child nodes.
     */
    public RandomTreeGenerator(Random rand,
                               BiFunction <T, List<Node>, Node> nodeGenerator) {
        this.rand = rand;
        this.nodeGenerator = nodeGenerator;
    }

    /**
     * @param dataElements elements are injected in pre-order.
     * @param maxChildNum The maximum number of child nodes a node can have.
     */
    public Tree<T,Node> generateTreeWithRandomStructure(List<T> dataElements, int maxChildNum) {
        if (dataElements.isEmpty())
            throw new RuntimeException("argument can't be empty");
        Tree<T,Node> $ = new Tree<>(generateNodeAndDescendantsWithRandomStructure(dataElements, maxChildNum));
        assert ($.getAllNodesPreOrder().size() == dataElements.size());
        return $;
    }


    private Node generateNodeAndDescendantsWithRandomStructure(List<T> dataElements, int maxChildNum) {

        assert(dataElements.size()>0);

        List<Node> children = new LinkedList<>();


        if (dataElements.size()==1)
            return nodeGenerator.apply(dataElements.get(0), children);

        // The first constant is for the generated root node (so can't use it for children).
        int childsNum = 1+rand.nextInt(Math.min(dataElements.size(), maxChildNum +1)-1);

        int descendantsNumOfChild[] = new int[childsNum];
        Arrays.fill(descendantsNumOfChild,1);
        for(int i=0;i<dataElements.size()-childsNum-1;i++)
            descendantsNumOfChild[rand.nextInt(childsNum)]++;


        int constStartInd = 1; // 0 is for the root node.
        for(int i=0;i<childsNum;i++) {
            int constEndInd = constStartInd + descendantsNumOfChild[i];
            assert(constEndInd<=dataElements.size());
            Node newNode = generateNodeAndDescendantsWithRandomStructure(
                    dataElements.subList(constStartInd, constEndInd), maxChildNum);
            //noinspection unchecked
            children.add(newNode);
            constStartInd = constEndInd;
        }

        return nodeGenerator.apply(dataElements.get(0), children);
    }
}
