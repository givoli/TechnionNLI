package il.ac.technion.nlp.nli.core.state;


import il.ac.technion.nlp.nli.core.reflection.EntityGraphReflection;

/**
 * NLI Entity is a synonym for non-primitive entity.
 *
 * A non primitive entity is one for which there may be triples in the database graph where the first element of the
 * tipple is that entity.
 *
 * To be used by the developer-user (for their own types).
 *
 * The objects of subtypes of {@link NliEntity} contain the data about relations (x,y)  s.t. x is the entity
 * represented by this object, and y is an entity.
 * Every field which is its type is an entity type defines a relation:
 * If the field contains null, then it represents the absence of a triple in the graph KB.
 * A relation field may be a {@link java.util.List}.
 *      Note: If there are no equivalence checks done between entity graphs (e.g. to check if a final state is
 *      equivalent to the correct desired state), which are performed via
 *      {@link EntityGraphReflection#entityGraphEquals(NliRootEntity, NliRootEntity)}
 *      then a relation field can also be of type {@link java.util.Collection}.
 *
 * {@link java.util.Collection}. If the succesfulness If a deterministic functionality is required (e.g. for
 * deterministic experiment results), then the collection should be of type .
 *
 * IMPORTANT: since we use serialization to clone {@link NliEntity} subtype objects - all subtype fields must
 * not refer to objects outside the entities graph, unless defined as transient.
 *
 *
 *
 * TODO: resolve the issue that the users' class might not be serializable (but making it implement this interface requires it to be).
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public interface NliEntity extends Entity {

}
