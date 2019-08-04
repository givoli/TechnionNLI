package il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities;

import il.ac.technion.nlp.nli.core.state.NliEntity;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class Employee implements NliEntity {
    private static final long serialVersionUID = -8220137410563276675L;

    public String name;
    public Position position;
    public @Nullable Employee manager;
    public int salary;

    public Employee(String name, Position position, @Nullable Employee manager, int salary) {
        this.name = name;
        this.position = position;
        this.manager = manager;
        this.salary = salary;
    }

    @Override
    public boolean equals(Object o) {
       return super.equals(o);
    }

    /**
     * TODO: make this and equals use the fields - just for the sake of not having un-common behavior.
     */
    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
