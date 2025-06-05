package ofergivoli.olib.data_structures.tree;

import java.util.ArrayList;
import java.util.List;

/**
 * A node in {@link Tree}.
 * @param <T> The type of data contained in each node.
 */
//TODO: consider changing to TreeNode<T, Node extends TreeNode<T>>
public class TreeNode<T> {


    private final ArrayList<? extends TreeNode<T>> childNodes;
    public T data;


    /**
     * @param childNodes A shallow-copy is kept. May be null (equivalent to an empty list).
     */
    public TreeNode(T data, List<? extends TreeNode<T>> childNodes) {
        this.childNodes = (childNodes==null) ? new ArrayList<>() : new ArrayList<>(childNodes);
        this.data = data;
    }


    /**
     * @param multipleLines if false then the returned string is on a single line.
     */
    public String toLispTreeString(boolean multipleLines) {
        StringBuilder sb = new StringBuilder();
        toLispTreeString(sb, multipleLines);
        return sb.toString();
    }

    public void toLispTreeString(StringBuilder sb, boolean multipleLines) {
        toLispTreeString(sb, multipleLines, 0);
    }

    /**
     * @param sb The representation of the current node will be appended to sb (without adding any preceding
     *           spaces/newlines).
     * @param multipleLines if false then the returned string is on a single line.
     * @param indents The number of indents of the line we're starting appending to.
     */
    private void toLispTreeString(StringBuilder sb, boolean multipleLines, int indents) {

        if (!multipleLines)
            indents = 0;

        if (!childNodes.isEmpty())
            sb.append("(");

        sb.append("[").append(data.toString()).append("]");


        for (TreeNode<T> c : childNodes) {
            if (multipleLines) {
                sb.append("\n");
                appendIndentSpaces(sb, indents + 1);
            }
            else
                sb.append(" ");

            c.toLispTreeString(sb, multipleLines, indents+1);
        }

        if (!childNodes.isEmpty())
            sb.append(")");
    }

    private void appendIndentSpaces(StringBuilder sb, int indentsNum) {
        final int SPACES_PER_INDENT = 4;
        for (int i=0; i<indentsNum*SPACES_PER_INDENT; i++) {
            sb.append(" ");
        }
    }


    public boolean isLeaf() {
        return childNodes.size()==0;
    }


    /**
     * You may not modify structure.
     */
    public ArrayList<? extends TreeNode<T>> getChildNodes() {
        return childNodes;
    }

    @Override
    public final boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public final int hashCode() {
        return super.hashCode();
    }
}
