package il.ac.technion.nlp.nli.core;

import il.ac.technion.nlp.nli.core.state.NliEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to add a description (a natural language phrase) to a NLI method, relation field or a class implementing
 * {@link NliEntity}. Note that the identifier of the method/field/class already provides one description.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface NliDescriptions {

    /**
     * Order: from most to least important.
     */
    String[] descriptions();
}
