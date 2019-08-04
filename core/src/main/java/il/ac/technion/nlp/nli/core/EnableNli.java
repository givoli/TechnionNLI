package il.ac.technion.nlp.nli.core;

import il.ac.technion.nlp.nli.core.method_call.InvalidNliMethodInvocation;
import il.ac.technion.nlp.nli.core.state.NliEntity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used by the developer-user, for annotating their own methods in their own entities.
 *
 * This should annotate every interface method.
 * Annotated methods should belong to {@link NliEntity} classes (or classes that those
 * classes inherit from). Not to be used on static methods.
 *
 * Each argument of an annotated methods must be either an entity or {@link java.util.Set} of entities.
 * The return value is ignored.
 *
 * If using execution signal during inference, the implementation of the annotated method must not cause any side
 * effects other than modifying the entity graph. It may throw {@link RuntimeException} which represents a failure to
 * execute the method (which is a legitimate scenario that can provide a signal during inference that the arguments are
 * incorrect, but throwing {@link InvalidNliMethodInvocation} in these cases is more recommended because it won't
 * pollute the log).
 *
 * If the user requires deterministic results, the implementation of the annotated methods must be deterministic
 * (remember this when iterating over elements of an argument set in a non-deterministic order).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnableNli {
}
