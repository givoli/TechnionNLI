package il.ac.technion.nlp.nli.core.method_call;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.io.log.Log;
import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.PrimitiveEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.reflection.GeneralReflection;
import org.apache.commons.lang3.ClassUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the interface method + arguments.
 * Refers to {@link NliEntity} ids as defined by some state, but does not contain a field referring to that state.
 * When invoking the {@link MethodCall} the caller provides the state (which may be a deep-copy).
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class MethodCall implements Serializable {


	private static final long serialVersionUID = 6532258498538873005L;


    /**
     * The method id of the {@link EnableNli} annotated method.
     */
	private final MethodId methodId;

	/**
	 * Order by parameters definition order in the method.
	 */
	private final ArrayList<Argument> arguments;

    private final String entityIdOfObjectMethodIsInvokeOn;

    /**
     * @param entityIdOfObjectMethodIsInvokeOn The id (in the state) of the entity on which the interface method to be
     *                                         invoked.
     */
	public MethodCall(MethodId methodId, String entityIdOfObjectMethodIsInvokeOn, ArrayList<Argument> arguments) {
		this.methodId = methodId;
        this.arguments = arguments;
        this.entityIdOfObjectMethodIsInvokeOn = entityIdOfObjectMethodIsInvokeOn;
    }

	public MethodCall(MethodId methodId, String entityIdOfObjectMethodIsInvokeOn, Argument... arguments) {
		this(methodId, entityIdOfObjectMethodIsInvokeOn, new ArrayList<>(Arrays.asList(arguments)));
	}


    /**
     * @param initialState only used in order to verify the validity of types, not referred to by the returned object.
     * @return null in a valid function call can not be created because any of the following:
     *  - wrong size of 'arguments'.
     *  - invalid type of the entity to invoke the method on
     *  - invalid entities in the arguments:
     *      - invalid type.
     *      - not a single entity when a single value is expected.
     */
    public static @Nullable
    MethodCall createValidFunctionCall(MethodId methodId, String entityIdOfObjectMethodIsInvokeOn,
                                       ArrayList<Argument> arguments, State initialState) {

        if (arguments.size() != methodId.getParameterClassNames().size())
            return null;

        // validating type of object to invoke on
        NliEntity objToInvokeOn = initialState.getEntityById(entityIdOfObjectMethodIsInvokeOn);
        if (!methodId.getDeclaringClassCanonicalName().equals(objToInvokeOn.getClass().getCanonicalName()))
            return null;

        Method method = methodId.getMethod();
        // For each argument, validating types of all entities in it.
        for (int i=0; i<arguments.size(); i++) {
            Argument arg = arguments.get(i);
            Type paramType = method.getGenericParameterTypes()[i];

            if (!GeneralReflection.isUserEntityTypeCollection(paramType) //TODO: use a more efficient method.
                    && arg.size() != 1)
                return null; // expected an argument with single value

            Class<?> paramUserType = GeneralReflection.getUserType(paramType);

            if (arg instanceof PrimitiveArgument) {
                PrimitiveArgument primitiveArg = (PrimitiveArgument) arg;
                if (primitiveArg.getPrimitiveEntities().stream()
                        .anyMatch(e -> {
                            Class<?> clazz =  e.getValue() == null  ?  null : e.getValue().getClass();
                            return !ClassUtils.isAssignable(clazz, paramUserType);
                        }))
                    return null;
            } else {
                NonPrimitiveArgument nonPrimitiveArg = (NonPrimitiveArgument) arg;
                if (nonPrimitiveArg.getNonPrimitiveEntityIds().stream()
                        .map(initialState::getEntityById)
                        .anyMatch(e->!ClassUtils.isAssignable(e.getClass(), paramUserType)))
                    return null;
            }
        }

        // a valid function call can be created :)
        return new MethodCall(methodId, entityIdOfObjectMethodIsInvokeOn, arguments);
    }

	/**
	 *  See doc of {@link #invokeOnGivenState(State)}
	 *  @param state The state to on which this {@link MethodCall} is invoked on.
	 *  @return The entity value array representing the actual arguments to be used for invoking the NLI method.
	 */
	Object[] getArgumentValues(State state) {

        /**
         * This list is the result of mapping the elements of 'arguments' to collections containing strictly user entity
         * values. Primitive entities representing 'null' are ignored (so there are no null values in the returned
         * collections).
         * Element of index i in this list is a collection containing user entity values to be sent to the function as
         * argument i (whether parameter i is a collection or not, if not then the collection contains a single element
         * that needs to be stripped away).
         */
        ArrayList<List<?>> argCollections = arguments.stream().map(arg -> {

            if (arg instanceof PrimitiveArgument) {
                /** Collecting the user values from the {@link PrimitiveEntity}s */
                return ((PrimitiveArgument) arg).primitiveEntities.stream()
                        .map(PrimitiveEntity::getValue)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            } else if (arg instanceof NonPrimitiveArgument) {
                /** Collecting the {@link NliEntity}s matching the ids */
                Collection<String> entityIds = ((NonPrimitiveArgument) arg).nonPrimitiveEntityIds;
                return entityIds.stream()
                        .map(state::getEntityById)
                        .collect(Collectors.toList());
            } else
                throw new Error();
        }).collect(Collectors.toCollection(ArrayList::new));


        // In this list, element i is the actual value that will be passed as argument i to the function.
        List<Object> argValues = new LinkedList<>();
        for (int i=0; i<arguments.size(); i++) {
            List<?> argCollection = argCollections.get(i);
            if (GeneralReflection.isUserEntityTypeCollection(methodId.getMethod().getGenericParameterTypes()[i])) {
                // Keeping the collection as is.
                argValues.add(argCollection);
            } else  {
                // Stripping away the (singleton) collection.
                Verify.verify(argCollection.size() == 1);
                Object userValue = argCollection.get(0);
                assert(GeneralReflection.isUserEntityType(userValue.getClass()));
                argValues.add(userValue);
            }
        }

        return argValues.toArray();
	}



    /**
     * See {@link #invokeOnDeepCopyOfState(State)}
     * @param state to invoke on.
     * @return true iff invocation was successful.
     */
    private boolean invokeOnGivenState(State state) {

        NliEntity objToInvokeOn = state.getEntityById(entityIdOfObjectMethodIsInvokeOn);
        Verify.verify(objToInvokeOn != null);
        try {
            Method method = methodId.getMethod();
            method.setAccessible(true);
            method.invoke(objToInvokeOn, getArgumentValues(state));
			state.updateStateFollowingEntityGraphModifications();
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            Throwable exceptionThrown = e.getCause();
            if (exceptionThrown instanceof InvalidNliMethodInvocation)
                return false; // no need to log a warning, we expect many invocations to fail throwing this exception.
            Log.warn("An interface method threw a Throwable not of type InvalidNliMethodInvocation:\n" + exceptionThrown.toString());
            return false;
        }
        return true;
    }




	/**
     *
     * Invokes this function call on the root entity of a new deep copy of 'state'.
     * Also calls method.setAccessible(true).
     * @return On success: the new copy of the state after the execution of the SI function with it.
     * null is returned if the invocation failed, meaning that the invoked interface function threw some Throwable. We
     * expect this to happen often during inference. When this happen, assume the state is corrupted.
     */
	public @Nullable State invokeOnDeepCopyOfState(State state) {
		State copy = state.deepCopy();
        boolean success = invokeOnGivenState(copy);
        if (!success)
            return null;
		return copy;
	}

    public ArrayList<Argument> getArguments() {
        return arguments;
    }

    public MethodId getMethodId() {
        return methodId;
    }

    /**
     * For arguments with non-primitive values, the comparison is based on ids.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodCall that = (MethodCall) o;
        return Objects.equals(methodId, that.methodId) &&
                Objects.equals(arguments, that.arguments) &&
                Objects.equals(entityIdOfObjectMethodIsInvokeOn, that.entityIdOfObjectMethodIsInvokeOn);
    }

    /**
     * For arguments with non-primitive values, the hashCode depends on the relevant ids.
     */
    @Override
    public int hashCode() {
        return Objects.hash(methodId, arguments, entityIdOfObjectMethodIsInvokeOn);
    }
}
