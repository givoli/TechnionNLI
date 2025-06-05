package ofergivoli.olib.data_structures.tree;

import java.util.*;

/**
 * A generic tree with immutable structure. The children of a node are ordered.
 * Multiple trees can share the same nodes.
 * The link from each node to its parent is contained by the tree (not by the nodes).
 * Contract: All nodes in a given tree are distinct objects.
 * @param <T> The type of data contained in each node.
 * @param <Node> The type of the nodes.
 *
 */
public class Tree<T, Node extends TreeNode<T>> {


    public final Node root;

    public Tree(Node root) {
        this.root = root;
        initNodeToParentMap();
    }

    private void initNodeToParentMap() {
        //noinspection unchecked
        getAllNodesPreOrder().forEach(node->node.getChildNodes().forEach(child->nodeToParent.put((Node) child,node)));
        nodeToParent.put(root,null);
    }

    /**
     * The value of a key node is null if that node is the root.
     */
    private final IdentityHashMap<Node,Node> nodeToParent = new IdentityHashMap<>();


    /**
     * @param multipleLines if false then the returned string is on a single line.
     */
    public String toLispTreeString(boolean multipleLines) {

        StringBuilder sb = new StringBuilder();
        root.toLispTreeString(sb, multipleLines);
        if (multipleLines)
            sb.append("\n");
        return sb.toString();
    }


    /**
     * Note: Computed only once and then kept for future calls.
     * @return A list of all nodes. Order: pre-order (according to children order).
     * Read-only
     */
    public List<Node> getAllNodesPreOrder() {
        computeNodeDirectAccessDataIfMissing();
        return Collections.unmodifiableList(nodeDirectAccessData.allNodesPreOrder);
    }


    /**
     * Note: Computed only once and then kept for future calls.
     * @return A list of all leaf (natural order defined by the children order of each node).
     */
    public ArrayList<Node> getLeafs() {
        computeNodeDirectAccessDataIfMissing();
        return nodeDirectAccessData.leafs;
    }


    public TreePath<T,Node> getPathBetweenTwoNodes(Node from, Node to) {
        Node commonAncestor = findCommonAncestor(from,to);
        return new TreePath<>(this, from, to, commonAncestor);
    }

    public Node findCommonAncestor(Node a, Node b) {

        // TODO: use so IdentityHashSet implementation instead of IdentityHashMap (value is unneeded).
        IdentityHashMap<Node,Object> visitedNodes = new IdentityHashMap<>();

        Node curr = a;
        while (curr != null) {
            visitedNodes.put(curr,null);
            //noinspection unchecked
            curr = getParentNode(curr);
        }

        curr = b;
        while (!visitedNodes.containsKey(curr))
            //noinspection unchecked
            curr = getParentNode(curr);

        return curr;
    }

    /**
     * @return null if 'node' is the root.
     */
    public Node getParentNode(Node node) {
        return nodeToParent.get(node);
    }

    private static class NodeDirectAccessData<Node extends TreeNode<?>> {
        LinkedList<Node> allNodesPreOrder = new LinkedList<>();
        ArrayList<Node> leafs = new ArrayList<>();
    }

    /**
     * Computed once on first demand.
     */
    private NodeDirectAccessData<Node> nodeDirectAccessData;

    private void computeNodeDirectAccessDataIfMissing() {
        if (nodeDirectAccessData != null)
            return;
        nodeDirectAccessData = new NodeDirectAccessData<>();
        addNodeAndDescendantsToListPreOrder(root, nodeDirectAccessData.allNodesPreOrder, nodeDirectAccessData.leafs);
    }

    /**
     * Appends to 'allNodes' nodes under this node including this node itself. Order:  pre-order, according to the
     * children order.
     * Similarly appends to 'leafs' all the leafs under 'node' (including itself - if its a leaf).
     */
    private void addNodeAndDescendantsToListPreOrder(Node node, LinkedList<Node> allNodes, ArrayList<Node> leafs) {

        allNodes.add(node);
        if (node.isLeaf())
            leafs.add(node);
        //noinspection unchecked
        node.getChildNodes().forEach(c -> addNodeAndDescendantsToListPreOrder((Node) c,allNodes,leafs));
    }


    /**
     * @return the index of 'child' among the children of its parent.
     * @throws RuntimeException in case 'child' is the root.
     */
    public int getChildIndex(Node child) {
        TreeNode parent = getParentNode(child);
        if (parent == null)
            throw new RuntimeException("'child' is a root");
        for (int i = 0; i< parent.getChildNodes().size(); i++)
            if (parent.getChildNodes().get(i) == child)
                return i;
        throw new Error();
    }

}
