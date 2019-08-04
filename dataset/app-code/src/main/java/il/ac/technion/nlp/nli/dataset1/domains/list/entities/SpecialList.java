package il.ac.technion.nlp.nli.dataset1.domains.list.entities;

import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.NliDescriptions;
import il.ac.technion.nlp.nli.core.method_call.InvalidNliMethodInvocation;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class SpecialList implements NliRootEntity {

    private static final long serialVersionUID = 6347291665635296045L;

    /**
     * The data to manage.
     */
    private LinkedList<ListElement> elements = new LinkedList<>();

    @SuppressWarnings("FieldCanBeLocal")
    // The following two fields are required due to the list order not being used to determine whether two states are
    // identical.
    private @Nullable ListElement firstElement;
    @SuppressWarnings("FieldCanBeLocal")
    private @Nullable ListElement lastElement;

    public void addElement(ListElement e) {
        elements.add(e);
        updateFirstAndLastElements();
    }

    private void updateFirstAndLastElements() {
        firstElement = elements.isEmpty() ? null : elements.getFirst();
        lastElement = elements.isEmpty() ? null : elements.getLast();
    }

    @NliDescriptions(descriptions = {"remove", "delete"})
    @EnableNli
    public void remove(Collection<ListElement> c) {
        elements.removeAll(c);
        updateFirstAndLastElements();
    }

    @NliDescriptions(descriptions = {"move", "beginning", "start"})
    @EnableNli
    public void moveToBeginning(ListElement number) {
        if (!elements.contains(number))
            throw new InvalidNliMethodInvocation();
        elements.remove(number);
        elements.addFirst(number);
        updateFirstAndLastElements();
    }


    @NliDescriptions(descriptions = {"move", "end"})
    @EnableNli
    public void moveToEnd(ListElement number) {
        if (!elements.contains(number))
            throw new InvalidNliMethodInvocation();
        elements.remove(number);
        elements.addLast(number);
        updateFirstAndLastElements();
    }

    public LinkedList<ListElement> getElements() {
        return elements;
    }
}
