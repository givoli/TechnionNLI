package il.ac.technion.nlp.nli.parser.denotation;

import com.google.common.base.Verify;
import edu.stanford.nlp.sempre.*;
import edu.stanford.nlp.sempre.tables.lambdadcs.ExplicitUnaryDenotation;
import edu.stanford.nlp.sempre.tables.lambdadcs.LambdaDCSCoreLogic;
import edu.stanford.nlp.sempre.tables.lambdadcs.TypeHint;
import edu.stanford.nlp.sempre.tables.lambdadcs.UnaryDenotation;
import il.ac.technion.nlp.nli.core.method_call.*;
import il.ac.technion.nlp.nli.core.reflection.GeneralReflection;
import il.ac.technion.nlp.nli.core.state.*;
import il.ac.technion.nlp.nli.parser.experiment.ExperimentRunner;
import il.ac.technion.nlp.nli.parser.InstructionKnowledgeGraph;
import il.ac.technion.nlp.nli.parser.NliMethodCallFormula;
import il.ac.technion.nlp.nli.parser.NameValuesManager;
import il.ac.technion.nlp.nli.parser.general.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class DenotationUtils {


    public static boolean firstArgumentIsObjToInvokeMethodOn(MethodId methodId) {
        return !NliRootEntity.class.isAssignableFrom(methodId.getMethod().getDeclaringClass());
    }


    /**
     * @param argsIncludingObjToInvokeOnAsUD each element is a UnaryDenotation representing a collection of
     *                                       {@link Entity}s. The first element may or may not represent the object
     *                                       on which the NLI method is to be invoke on, as defined by
     *                                       {@link #firstArgumentIsObjToInvokeMethodOn}.
     * @return A UnaryDenotation, which is a singleton containing a {@link LazyStateValue} representing the resulting
     * state of the {@link MethodCall}.
     * Returns an empty ExplicitUnaryDenotation in case a valid one can't be created (e.g. because of incorrect number
     * of arguments).
     */
    private static ExplicitUnaryDenotation createLazyStateDenotationAsSingletonUnaryDenotation(
            InstructionKnowledgeGraph graph, String friendlyIdOfNliMethod,
            List<UnaryDenotation> argsIncludingObjToInvokeOnAsUD) {

        State initialState = graph.getInitialState();
        MethodId methodId = initialState.getDomain().getFriendlyIdToMethodId().safeGet(friendlyIdOfNliMethod);
        Method method = methodId.getMethod();
        Type[] methodParamGenericTypes = method.getGenericParameterTypes();
        boolean firstArgumentIsObjToInvokeMethodOn = firstArgumentIsObjToInvokeMethodOn(methodId);
        if (argsIncludingObjToInvokeOnAsUD.size() !=
                (firstArgumentIsObjToInvokeMethodOn ? methodParamGenericTypes.length + 1 :
                                                      methodParamGenericTypes.length))
            return new ExplicitUnaryDenotation(); // wrong arguments number.

        List<Argument> argsIncludingPerhapsObjToInvokeOn = new ArrayList<>();
        int argIndex = firstArgumentIsObjToInvokeMethodOn ? -1 : 0;
        for (UnaryDenotation argAsUD : argsIncludingObjToInvokeOnAsUD) {
            Class<?> userValueType;
            if (argIndex == -1)
                userValueType = GeneralReflection.getUserType(method.getGenericReturnType());
            else
                userValueType = GeneralReflection.getUserType(methodParamGenericTypes[argIndex]);
            Verify.verify(userValueType != null);
            Argument arg = createArg(graph, argAsUD, userValueType);
            if (arg == null)
                return new ExplicitUnaryDenotation();
            argsIncludingPerhapsObjToInvokeOn.add(arg);
            argIndex++;
        }

        MethodCall methodCall = createFunctionCall(initialState, methodId, argsIncludingPerhapsObjToInvokeOn);
        if (methodCall == null)
            return new ExplicitUnaryDenotation();

        return new ExplicitUnaryDenotation(new LazyStateValue(initialState, methodCall));
    }


    /**
     * @return a singleton, where the value is of type {@link LazyStateValue}.
     */
    public static UnaryDenotation createLazyStateDenotationAsSingletonUnaryDenotation(
            LambdaDCSCoreLogic lambdaDCSCoreLogic, NliMethodCallFormula functionCall) {

        // We are not currently using typeHint.

        List<UnaryDenotation> argsIncludingPerhapsObjToInvoke = functionCall.getArgIncludingPerhapsObjToInvokeOn()
                .stream()
                .map(formula->lambdaDCSCoreLogic.computeUnary(formula, TypeHint.UNRESTRICTED_UNARY))
                .collect(Collectors.toList());

        return DenotationUtils.createLazyStateDenotationAsSingletonUnaryDenotation(
                (InstructionKnowledgeGraph) lambdaDCSCoreLogic.graph,
                functionCall.getFriendlyIdOfMethod(), argsIncludingPerhapsObjToInvoke);
    }


    /**
     * @param argsIncludingPerhapsObjToInvokeOn same documentation as for
     * {@link #createLazyStateDenotationAsSingletonUnaryDenotation}.
     * @return null in case a valid function call could not be created.
     */
    static @Nullable
    MethodCall createFunctionCall(State initialState, MethodId methodId,
                                  List<Argument> argsIncludingPerhapsObjToInvokeOn) {

        String idOfObjToInvokeOn;
        ArrayList<Argument> args = new ArrayList<>();
        if (firstArgumentIsObjToInvokeMethodOn(methodId))
        {
            NonPrimitiveArgument firstArg = (NonPrimitiveArgument) argsIncludingPerhapsObjToInvokeOn.get(0);
            if (firstArg.getNonPrimitiveEntityIds().size() != 1)
                return null; // expected a first argument representing a single object to invoke the method on.
            idOfObjToInvokeOn = firstArg.getNonPrimitiveEntityIds().iterator().next();
            if (argsIncludingPerhapsObjToInvokeOn.size()>1)
                args.addAll(argsIncludingPerhapsObjToInvokeOn.subList(1, argsIncludingPerhapsObjToInvokeOn.size()));

        } else {
            idOfObjToInvokeOn = initialState.getEntityId(initialState.getRootEntity());
            args.addAll(argsIncludingPerhapsObjToInvokeOn);
        }

        return MethodCall.createValidFunctionCall(methodId, idOfObjToInvokeOn, args, initialState);
    }




    /**
     * @return null in case a valid {@link Argument} with value type of 'userValueType' can't be created.
     */
    private static @Nullable Argument createArg(InstructionKnowledgeGraph graph, UnaryDenotation argAsUD,
                                                Class<?> userValueType) {

        if (argAsUD.isEmpty())
            return new EmptyArgument();

        if (NliEntity.class.isAssignableFrom(userValueType)){
            // trying to create a NonPrimitiveArgument:
            List<String> nliEntityIds = new LinkedList<>();
            for (Value value : argAsUD) {
                if (!(value instanceof NameValue) || graph.nameValuesManager.getNameValueType((NameValue) value)
                        != NameValuesManager.NameValueType.NLI_ENTITY)
                    return null;

                String nliEntityId = graph.nameValuesManager.getNliEntityId((NameValue) value);
                if(!graph.initialState.getEntityById(nliEntityId).getClass().equals(userValueType))
                    return null;

                nliEntityIds.add(nliEntityId);
            }
            return new NonPrimitiveArgument(nliEntityIds);
        } else {
            // trying to create a PrimitiveArgument:
            List<PrimitiveEntity> primitiveEntities = new LinkedList<>();
            for (Value value : argAsUD) {
                PrimitiveEntity primitiveEntity =
                        graph.createPrimitiveEntityFromSempreValue(value, userValueType);
                if (primitiveEntity == null)
                    return null;
                primitiveEntities.add(primitiveEntity);
            }
            return PrimitiveArgument.createFromEntities(primitiveEntities);
        }
    }

    /**
     * @throws RuntimeException in case 'derivation' does not denote a {@link LazyStateValue} (note that this does
     * not require for a valid denoted {@link State}).
     */
    public static @NotNull LazyStateValue getEndStateValueDenotedByDerivation(
            Example sempreExample, Derivation derivation) {

        Verify.verify(Utils.derivationIsRoot(sempreExample, derivation));

        Value value = getDenotationOfDerivation(sempreExample, derivation);
        if (!(value instanceof ListValue))
            throw new RuntimeException("value is not a ListValue");
        List<Value> valueList = ((ListValue) value).values;
        if (valueList == null || valueList.size() != 1 || !(valueList.get(0) instanceof StateValue))
            throw new RuntimeException("value list does not contain a single StateValue");
        return (LazyStateValue) valueList.get(0);
    }

    public static @Nullable Value getDenotationOfDerivation(Example sempreExample, Derivation derivation) {
        Parser parser = ExperimentRunner.getCurrentExperiment().sempreParser;
        derivation.ensureExecuted(Objects.requireNonNull(parser).executor, sempreExample.context);
        return derivation.value;
    }
}
