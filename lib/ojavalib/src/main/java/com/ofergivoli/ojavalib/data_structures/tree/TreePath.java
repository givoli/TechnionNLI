package com.ofergivoli.ojavalib.data_structures.tree;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Directed path between two tree nodes.
 * @param <T> The data type in each node.
 * @param <Node> The node type.
 *
 */
public class TreePath<T, Node extends TreeNode<T>> {

    public final Node from;
    public final Node to;
    public final Tree<T, Node> tree;

    /**
     * Might be 'from' or 'to' nodes.
     */
    public final Node commonAncestor;



    /**
     * The nodes on the path (including 'from' and 'to' nodes; they might be the same node).
     */
    public final ArrayList<Node> pathNodes;

    /**
     * index of the common ancestor node in {@link #pathNodes}.
     */
    public final int commonAncestorInd;


    /**
     * Returns the values for {@link #pathNodes} and {@link #commonAncestorInd}.
     */
    private Pair<ArrayList<Node>, Integer> generatePath(Node from, Node to, Node commonAncestor) {
        ArrayList<Node> $ = new ArrayList<>();

        Node node = from;
        int ancestorInd=0;
        while (node != commonAncestor) {
            $.add(node);
            //noinspection unchecked
            node = tree.getParentNode(node);
            ancestorInd++;
        }
        $.add(node); // adding ancestor

        LinkedList<Node> secondPartOfPath = new LinkedList<>(); // excludes the ancestor
            // from node after ancestor to 'to' node..
        node = to;
        while (node != commonAncestor) {
            secondPartOfPath.addFirst(node);
            //noinspection unchecked
            node = tree.getParentNode(node);
        }

        $.addAll(secondPartOfPath);

        return new ImmutablePair<>($,ancestorInd);
    }

    /**
     * Returns the number of edges in the path.
     */
    public int getPathLength() {
        return pathNodes.size()-1;
    }

    public TreePath(Tree<T, Node> tree, Node from, Node to, Node commonAncestor) {
        this.tree = tree;
        this.from = from;
        this.to = to;
        this.commonAncestor = commonAncestor;

        Pair<ArrayList<Node>, Integer> $ = generatePath(from, to, commonAncestor);
        this.pathNodes = $.getLeft();
        this.commonAncestorInd = $.getRight();
    }

}
