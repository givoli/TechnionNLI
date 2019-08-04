package il.ac.technion.nlp.nli.dataset1.domains.workforce_management.entities;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public enum Position {
    DEVELOPER, QA, MANAGER;

    @Override
    public String toString() {
        if (this == QA)
            return "QA";
        return name().toLowerCase();
    }
}

