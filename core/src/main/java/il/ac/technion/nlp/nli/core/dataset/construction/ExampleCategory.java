package il.ac.technion.nlp.nli.core.dataset.construction;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */

import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitConstructionInfo;
import il.ac.technion.nlp.nli.core.method_call.MethodId;

import java.io.Serializable;
import java.util.Objects;

/**
 * Used for grouping together examples of the same category, and randomly splitting each category separately to
 * train/test.
 */
public class ExampleCategory  implements Serializable {

    private static final long serialVersionUID = 3854836854114831700L;

    // TODO: add here a field for 'domain id' if multiple domains may contains the the same MethodId as NLI method.

    public final MethodId nliMethod;
    public final InstructionQueryability instructionQueryability;

    public ExampleCategory(HitConstructionInfo info) {
        this.nliMethod = info.nliMethod;
        instructionQueryability = InstructionQueryability.getByHitConstructionInfo(info);
    }

    public ExampleCategory(MethodId nliMethod, InstructionQueryability instructionQueryability) {
        this.nliMethod = nliMethod;
        this.instructionQueryability = instructionQueryability;
    }

    /**
     * returns true iff the two are the same category.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExampleCategory that = (ExampleCategory) o;
        return Objects.equals(nliMethod, that.nliMethod) &&
                instructionQueryability == that.instructionQueryability;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nliMethod, instructionQueryability);
    }

    @Override
    public String toString() {
        return "nliMethod=" + nliMethod.getName() +
                ", instructionQueryability=" + instructionQueryability;
    }
}