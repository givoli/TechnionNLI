package il.ac.technion.nlp.nli.core.state;

import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * May represent a null value (in that case, the value has no type).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class PrimitiveEntity implements Entity {

    private static final long serialVersionUID = 4091984174688416922L;


    /**
     * @see #isPrimitiveEntityType(Class)
     * This list contains only reference types (not java-primitive types), even though java-primitive types may serve as
     * primitive entity types.
     * A type is considered a primitive entity type iff one of the types in this list is assignable from it.
     *
     * MAINTANANCE RULE: a javadoc comment referring to this static field must be placed in every place that needs to be
     * updated when this list is updated.
     */
    private final static Collection<Class<?>> PRIMITIVE_ENTITY_TYPES = Arrays.asList(
            Integer.class, Double.class, Boolean.class, String.class, ZonedDateTime.class, Enum.class);

    /**
     * See: {@link #isScalarEntityType}
     * This code needs to be updated when {@link #PRIMITIVE_ENTITY_TYPES} changes.
     */
    private final static Collection<Class<?>> SCALAR_PRIMITIVE_ENTITY_TYPES = Arrays.asList(
            Integer.class, Double.class, ZonedDateTime.class);


    /**
     * {@link #isPrimitiveEntityType(Class)} must return true for the type of this value.
     */
    private final @Nullable Object value;

    public @Nullable Object getValue() {
        return value;
    }


    /**
     * @throws RuntimeException if the type of 'value' is not a valid primitive entity type.
     */
    public PrimitiveEntity(@Nullable Object value) {
        if (value != null && !isPrimitiveEntityType(value.getClass()))
            throw new RuntimeException("Type of value is not a valid primitive entity type");
        this.value = value;
    }

    /**
     * A primitive entity type has the following requirements:
     *  1. The hashCode() and equals() method must be deterministically implemented (and make sense with regard to the
     *  semantics of the type).
     *  2. The toString() method should return the pre-processed string to be used as the primitive entity name during
     *     inference.
     */
    public static boolean isPrimitiveEntityType(Class<?> type) {
        // note that 'type' might be a primitive type.
        return PRIMITIVE_ENTITY_TYPES.stream().anyMatch(t->ClassUtils.isAssignable(type,t));
    }


    /**
     * @return true iff the type of this primitive entity is one for which comparison operations can be performed.
     */
    public boolean isScalarEntityType() {
        return value != null &&
                SCALAR_PRIMITIVE_ENTITY_TYPES.stream().anyMatch(t -> ClassUtils.isAssignable(value.getClass(), t));
    }

    /**
     * For scalar entities only.
     * For two scalar entities e1 and e2, suppose this method returns x1,x2 respectively. Then x1>x2 iff e1>e2.
     * Note: For checking if two scalar entities are equal we use {@link {@link PrimitiveEntity#equals(Object)} and not
     * this method.
     * @throws RuntimeException if the type is not scalar.
     */
    public double mapToRealValueConservingComparisonRelations() {
        if (!isScalarEntityType())
            throw new RuntimeException("Not scalar entity type!");

        /**
         * This code needs to be updated when {@link #PRIMITIVE_ENTITY_TYPES} changes.
         */

        if (value instanceof Integer)
            return (Integer) value;

        if (value instanceof Double)
            return (Double) value;

        //noinspection ConstantConditions
        if (ZonedDateTime.class.isAssignableFrom(value.getClass())) {
            ZonedDateTime time = (ZonedDateTime) this.value;
            double nano = (double) time.getNano() / NANOSEC_PER_SER;
            assert(nano < 1.0);
            return time.toEpochSecond() + nano;
        }

        throw new Error();
    }


    /**
     * For scalar entity type only.
     * @throws RuntimeException if the two types are not the same and scalar.
     */
    public boolean isGreaterThan(PrimitiveEntity e) {

        if (!isTwoArgsAreScalarAndOfSameType(this,e))
            throw new RuntimeException("Not scalar type!");

        return mapToRealValueConservingComparisonRelations() > e.mapToRealValueConservingComparisonRelations();
    }


    /**
     * For scalar entity type only.
     * @throws RuntimeException if the two types are not the same and scalar.
     */
    public boolean isGreaterOrEqualTo(PrimitiveEntity e) {
        return !isSmallerThan(e);
    }

    /**
     * For scalar entity type only.
     * @throws RuntimeException if the two types are not the same and scalar.
     */
    public boolean isSmallerOrEqualTo(PrimitiveEntity e) {
        return !isGreaterThan(e);
    }



    /**
     * For scalar entity type only.
     * @throws RuntimeException if the two types are not the same and scalar.
     */
    public boolean isSmallerThan(PrimitiveEntity e) {
        return e.isGreaterThan(this) ;
    }

    private static boolean isTwoArgsAreScalarAndOfSameType(PrimitiveEntity a, PrimitiveEntity b) {
        return a.value != null  &&  b.value != null  &&
                a.value.getClass().equals(b.value.getClass()) &&
                a.isScalarEntityType();
    }

    /**
     * Deterministically implemented. Depends on the value type.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof PrimitiveEntity
                && Objects.equals(value, ((PrimitiveEntity) obj).value);
    }

    /**
     * Deterministically implemented.  Depends on the value type.
     */
    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }


    @Override
    public String toString(){

        if (value == null)
            return "";

        /**
         * This code needs to be updated when {@link #PRIMITIVE_ENTITY_TYPES} changes.
         */

        //TODO: consider having special treatment for Double, using perhaps DecimalFormat to make sure the string will be different for different Double values that are very similar.
        //TODO: consider having special treatment for ZonedDateTime, to make sure the string will be different for different nano sec values.

        if (Enum.class.isAssignableFrom(value.getClass()))
            return ((Enum)value).name();

        return value.toString();
    }


    private final static double NANOSEC_PER_SER = Math.pow(10, 9);

    public String getPreprocessedRelationStringForInference() {
        return toString();
    }

    public boolean isEnumValue() {
        return value != null && value.getClass().isEnum();
    }

    /**
     * Can be used to deterministically sort primitive entities deterministically.
     */
    public String getUniqueDeterministicStringDefiningEntity() {
        String classString =  getValue()==null ? "" :
                getValue().getClass().getCanonicalName();
        return classString + ":" + getValue();
    }

}
