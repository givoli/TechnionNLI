package il.ac.technion.nlp.nli.core.state;

import il.ac.technion.nlp.nli.core.EnableNli;

/**
 * To be used by the developer-user (for their own types).
 *
 * The entities in the state are the ones reachable from the root entity.
 * Also contains the interface methods, represented by methods annotated with
 * {@link EnableNli} (including inherited ones, access modifier does not matter).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public interface NliRootEntity extends NliEntity {
}
