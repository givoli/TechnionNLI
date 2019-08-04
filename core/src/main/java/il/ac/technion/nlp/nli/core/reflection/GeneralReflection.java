package il.ac.technion.nlp.nli.core.reflection;

import com.ofergivoli.ojavalib.io.log.Log;
import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.PrimitiveEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class GeneralReflection {

    /**
     * Checks whether a given class used by the user (as a relation field or {@link EnableNli}) method argument) is a
     * valid representation of a single entity.
     */
    public static boolean isUserEntityType(Class<?> clazz) {
        return NliEntity.class.isAssignableFrom(clazz) || PrimitiveEntity.isPrimitiveEntityType(clazz);
    }

    /**
     * @return The Class defining the class/interface of 'type' if there is one, or null if 'type' is something with
     * no defined class/interface (i.e. GenericArrayType,  TypeVariable, WildcardType).
     * NOTE: in Java {@link Class} cannot represent an array whose elements are of a generic type. So this method
     * returns null for GenericArrayType.
     */
    public static @Nullable Class<?> getClassOfType(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        } else if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();

            if (!(rawType instanceof Class)) {
                Log.warn("Did not expect ParameterizedType.getRawType to return something other than Class. The type:\n" + rawType);
                return null;
            }
            return (Class<?>) rawType;
        } else {
            return null;
        }
    }




    /**
     * @param potentialCollectionType Any non-null value is a valid input (doesn't even have to be a collection).
     * @return The class of entities in 'potentialCollectionType' if it's a collection of elements of "user entity type".
     * (see {@link #isUserEntityType(Class)}
     * Returns null in case 'potentialCollectionType':
     * - is a user entity type (note that theoretically {@link NliEntity} might extend {@link Collection} and in this
     * case we don't want to consider it as a collection of user entity type elements).
     * - does not represent a {@link Collection} with a single parametrized type argument that can be represented by a
     * {@link Class} (see {@link #getClassOfType(Type)}).
     */
    public static @Nullable Class<?> getUserTypeOfUserTypeCollection(Type potentialCollectionType) {

        Class<?> collectionClass = getClassOfType(potentialCollectionType);
        if (collectionClass == null)
            return null;

        if (isUserEntityType(collectionClass))
            return null; /** Note: {@link NliEntity} might extend {@link Collection} */

        if (!Collection.class.isAssignableFrom(collectionClass))
            return null;

        if (!(potentialCollectionType instanceof ParameterizedType))
            return null; // The type is a collection object which is not a parametrized type.

        Type[] types = ((ParameterizedType) potentialCollectionType).getActualTypeArguments();
        if (types.length != 1)
            return null; // The type is a collection object which is a parametrized type with more than one parametrized types.

        Class<?> clazz = getClassOfType(types[0]);
        if (!isUserEntityType(clazz))
            return null; // The elements of the collection are not of a user entity type.

        return clazz;
    }


    /**
     * Any non-null value is a valid input
     */
    public static boolean isUserEntityTypeCollection(Type type) {
        return getUserTypeOfUserTypeCollection(type) != null;
    }

    public static boolean isUserEntityTypeOrCollectionThereof(Type type) {
        return getUserType(type) != null;
    }


    /**
     * @param genericUserTypeOrCollection Represents either a generic user type or a generic collection type in which
     *                                    the type of the elements is a user type. Or any other type (in which case null
     *                                    is returned).
     * @return null if 'genericUserTypeOrCollection' is not a type representing a user entity type or a collection
     * of user entity type elements. Otherwise returns the class of a single user entity type.
     */
    public static @Nullable Class<?> getUserType(Type genericUserTypeOrCollection) {

        Class clazz = getClassOfType(genericUserTypeOrCollection);

        /** Notice that this check must come first in case there's an {@link NliEntity} which is also a
         * {@link Collection} subtype. */
        if (isUserEntityType(clazz))
            return clazz;

        // The only hope now is that 'genericUserTypeOrCollection' represents a collection.
        Class<?> elementClass = getUserTypeOfUserTypeCollection(genericUserTypeOrCollection);
        if (elementClass == null)
            return null;

        // 'relationField' is a collection.
        if (isUserEntityType(elementClass))
            return elementClass;
        else
            return null;
    }


    public static String getUniqueDeterministicStringForField(Field field) {
        return field.getType().getCanonicalName() + ":" + field.getName();
    }
}
