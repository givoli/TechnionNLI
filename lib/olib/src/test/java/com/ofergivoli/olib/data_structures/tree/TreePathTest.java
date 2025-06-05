package ofergivoli.olib.data_structures.tree;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertTrue;


public class TreePathTest {
    @Test
    public void testPathCorrectness() throws Exception {

        Random r = new Random(0);
        for (int i=0; i<10; i++) {
            Tree<Integer,TreeNode<Integer>> tree = new RandomIntTreeGenerator(r).generateRandomTree(1+i*5,5);
            ArrayList<TreeNode<Integer>> nodes = new ArrayList<>(tree.getAllNodesPreOrder());

            TreeNode<Integer> from = nodes.get(r.nextInt(nodes.size()));
            TreeNode<Integer> to = nodes.get(r.nextInt(nodes.size()));

            TreePath<Integer,TreeNode<Integer>> tp = tree.getPathBetweenTwoNodes(from,to);

            assertTrue(from == tp.from);
            assertTrue(to == tp.to);
            assertTrue(tp.pathNodes.get(tp.commonAncestorInd) == tp.commonAncestor);


            for (int j=0; j<tp.commonAncestorInd; j++) {
                assertTrue(tree.getParentNode(tp.pathNodes.get(j)) == tp.pathNodes.get(j+1));
            }

            for (int j = tp.commonAncestorInd+1; j<tp.pathNodes.size(); j++) {
                assertTrue(tree.getParentNode(tp.pathNodes.get(j)) == tp.pathNodes.get(j-1));
            }
        }

    }
}