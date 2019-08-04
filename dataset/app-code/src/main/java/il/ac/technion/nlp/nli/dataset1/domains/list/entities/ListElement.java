package il.ac.technion.nlp.nli.dataset1.domains.list.entities;

import il.ac.technion.nlp.nli.core.state.NliEntity;

import java.util.Objects;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ListElement implements NliEntity {

    private static final long serialVersionUID = -3996569908326668331L;

    /**
     * In the future this class may container more fields.
     * At the moment this is a simple int container.
     */
    public int value;

    public ListElement(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListElement that = (ListElement) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
