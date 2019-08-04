package il.ac.technion.nlp.nli.parser.kb;

import com.google.common.base.Verify;
import com.ofergivoli.ojavalib.data_structures.map.SafeHashMap;
import com.ofergivoli.ojavalib.data_structures.map.SafeMap;
import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import com.ofergivoli.ojavalib.io.csv.CsvContent;
import com.ofergivoli.ojavalib.string.StringManager;
import edu.stanford.nlp.sempre.*;
import il.ac.technion.nlp.nli.core.state.knowledgebase.GraphKb;
import il.ac.technion.nlp.nli.parser.NameValuesManager;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static il.ac.technion.nlp.nli.parser.NameValuesManager.NameValueType;

/**
 * A KB that represents relations and entities as {@link NameValue} and {@link Value} objects.
 * Note: an instance of this class may contain more data than a {@link GraphKb}, such as the order of entities in
 * a field list.
 *
 * Implementation note: the data structures in this class where chosen to allow efficient access during
 * inference.
 */
public class GraphKbWithSempreTypes {

    /**
     * @see #getRelationToFirstArgToSecondArgs()
     */
    private final SafeMap<NameValue,SafeMap<NameValue,SafeSet<Value>>> relationToFirstArgToSecondArgs
            = new SafeHashMap<>();

    /**
     * @see #getRelationToSecondArgToFirstArgs()
     */
    private final SafeMap<NameValue,SafeMap<Value,SafeSet<NameValue>>> relationToSecondArgToFirstArgs
            = new SafeHashMap<>();


    public void addFact(NameValue relation, NameValue firstArg, Value secondArg){
        addEntryToMap(relationToFirstArgToSecondArgs, relation, firstArg, secondArg);
        addEntryToMap(relationToSecondArgToFirstArgs, relation, secondArg, firstArg);
    }

    /**
     * Either "Arg A" represents first-arg and "Arg B" represents second-arg, or vice-versa.
     * @param relationToArgAToArgBs either {@link #relationToFirstArgToSecondArgs} or
     * {@link #relationToSecondArgToFirstArgs}.
     */
    private <A extends Value, B extends Value> void addEntryToMap(
            SafeMap<NameValue,SafeMap<A,SafeSet<B>>> relationToArgAToArgBs, NameValue relation, A argA, B argB){

        if (!relationToArgAToArgBs.safeContainsKey(relation))
            relationToArgAToArgBs.put(relation, new SafeHashMap<>());
        SafeMap<A,SafeSet<B>> argAToArgBs = relationToArgAToArgBs.safeGet(relation);
        if (!argAToArgBs.safeContainsKey(argA))
            argAToArgBs.put(argA, new SafeHashSet<>());
        SafeSet<B> argBs = argAToArgBs.getExisting(argA);
        boolean added = argBs.add(argB);
        Verify.verify(added);
    }



    /**
     * @throws RuntimeException in case the data structure is detected ass invalid.
     */
    public void verifyValidity(NameValuesManager nameValuesManager){
        verifyValidityOfMap(nameValuesManager, relationToFirstArgToSecondArgs);
        verifyValidityOfMap(nameValuesManager, relationToSecondArgToFirstArgs);
    }

    /**
     * @param relationToArgToArgs this should be either {@link #relationToFirstArgToSecondArgs} or
     * {@link #relationToSecondArgToFirstArgs}.
     * @throws RuntimeException in case a 'relationToArgToArgs' is detected as invalid.
     */
    private static void verifyValidityOfMap(
            NameValuesManager nameValuesManager,
            SafeMap<NameValue,? extends SafeMap<? extends Value, ? extends SafeSet<? extends Value>>>
                    relationToArgToArgs){

        //noinspection ConstantConditions
        relationToArgToArgs.keySet().forEach(relation->
            Verify.verify(nameValuesManager.isNameValueRepresentBinaryRelation(relation)));

        relationToArgToArgs.values().forEach(argToArgs->argToArgs.values().forEach(args->
                verifyAllValueElementsHaveSameTypeAndNameValueType(nameValuesManager, args)));
    }

    /**
     * Verifies that all elements in 'c' have the same type, and if that type is {@link NameValue} then also verifies
     * that the {@link NameValueType} of all the elements is the same.
     * @throws RuntimeException in case a violation of the above was detected.
     */
    private static void verifyAllValueElementsHaveSameTypeAndNameValueType(NameValuesManager nameValuesManager,
                                                                   Collection<? extends Value> c) {

        // This predicate will be applied to all the elements except the first, and will verify the element has
        // the same type as the first.
        Predicate<Value> predicate = null;
        for (Value element : c){
            if (predicate == null)
                predicate = e-> valuesBelongToSameClass(element, e) &&
                        (!(element instanceof NameValue) ||
                                nameValuesManager.getNameValueType((NameValue)e) ==
                                nameValuesManager.getNameValueType((NameValue)element));
            else
                if (!predicate.test(element))
                    throw new RuntimeException("Not all elements have the same type!");
        }

    }

    /**
     * @return true iff v1 and v2 return the same {@link #getClass()}, and in this context we consider
     * {@link DateValue} and {@link TimeValue} to be the same.
     */
    private static boolean valuesBelongToSameClass(Value v1, Value v2) {

        if (v1.getClass().equals(TimeValue.class) || v1.getClass().equals(DateValue.class))
            return v2.getClass().equals(TimeValue.class) || v2.getClass().equals(DateValue.class);

        return v1.getClass().equals(v2.getClass());
    }


    /**
     * Same documentation as for {@link #getRelationToSecondArgToFirstArgs()}
     */
    public SafeMap<NameValue, SafeMap<NameValue, SafeSet<Value>>> getRelationToFirstArgToSecondArgs() {
        return relationToFirstArgToSecondArgs;
    }


    /**
     * In the returned data structure:
     * - All {@link SafeMap} and {@link SafeSet} objects in the returned data structure are not empty.
     * - All the first arg values of the same relation are represented by the same {@link SemType}.
     * - All the second arg values of the same relation are represented by the same {@link SemType}.
     */
    public SafeMap<NameValue, SafeMap<Value, SafeSet<NameValue>>> getRelationToSecondArgToFirstArgs() {
        return relationToSecondArgToFirstArgs;
    }

    /**
     * @return a function that for every x returns all values y s.t. (x, relation, y) is a fact in the KB; or the
     * inverse of that function if 'inverse' is true.
     * The function never returns null.
     */
    public Function<Value, SafeSet<? extends Value>> getRelationAsFunction(NameValue relation, boolean inverse) {

        if(!getRelationToFirstArgToSecondArgs().safeContainsKey(relation))
            return ignored->null;

        if (inverse) {
            return secondArg -> {
                SafeSet<NameValue> result = getRelationToSecondArgToFirstArgs().getExisting(relation)
                        .safeGet(secondArg);
                return result == null ? new SafeHashSet<>() : result;
            };
        } else {
            return  firstArg-> {
                if (!(firstArg instanceof NameValue))
                    return new SafeHashSet<>();
                SafeSet<Value> result = getRelationToFirstArgToSecondArgs().getExisting(relation)
                        .safeGet((NameValue) firstArg);
                return result == null ? new SafeHashSet<>() : result;
            };
        }
    }


    public List<NameValue> getAllRelations(boolean deterministicOrder){
        Stream<NameValue> stream = relationToFirstArgToSecondArgs.keySet().stream();
        if (deterministicOrder)
            stream = stream.sorted(Comparator.comparing(nameValue->nameValue.id));
        return stream.collect(Collectors.toList());
    }


    public void writeToCsv(Path outCsv){
        CsvContent content = new CsvContent("First Argument", "Relation", "Second Argument");
        relationToFirstArgToSecondArgs.forEach((relation,firstArgToSecondArgs)->
                firstArgToSecondArgs.forEach((firstArg, secondArgs)->
                        secondArgs.forEach(secondArg->
                                content.addRow(firstArg.toString(), relation.toString(), secondArg.toString()))));

        content.writeEntireCsv(outCsv);
    }

    public List<KbTriple> createTripleList(boolean requireDeterministicOrder){
        List<KbTriple> result = new LinkedList<>();
        List<KbTriple> finalResult = result;
        relationToFirstArgToSecondArgs.forEach((relation, firstArgToSecondArgs)->
                firstArgToSecondArgs.forEach((firstArg, secondArgs)->
                        secondArgs.forEach(secondArg->
                                finalResult.add(new KbTriple(firstArg, relation, secondArg)))));
        if (requireDeterministicOrder)
            result = result.stream().sorted().collect(Collectors.toList());
        return result;
    }

    @Override
    public String toString() {


        List<ImmutableTriple<String, String, String>> triples = new LinkedList<>();

        relationToFirstArgToSecondArgs.forEach((relation, firstArgToSecondArgs)->
                firstArgToSecondArgs.forEach((firstArg, secondArgs)->{
                    secondArgs.forEach(secondArg->
                            triples.add(new ImmutableTriple<>(firstArg.toString(), relation.toString(),
                                    secondArg.toString())));
        }));

        return StringManager.collectionToStringWithNewlines(
                triples.stream()
                        .sorted()
                        .collect(Collectors.toList()));
    }
}
