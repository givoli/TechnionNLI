package il.ac.technion.nlp.nli.dataset1.domains.list.hit;

import com.google.common.base.Verify;
import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitRandomGenerator;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.method_call.NonPrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.dataset1.domains.list.entities.ListElement;
import il.ac.technion.nlp.nli.dataset1.domains.list.entities.SpecialList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ListHitRandomGenerator extends HitRandomGenerator{

    public ListHitRandomGenerator(Random rand, ListNliMethod nliMethod, DatasetDomains datasetDomains) {
        super(datasetDomains.getDomainByRootEntityClass(SpecialList.class), rand, ListStateVisualizer::new, nliMethod);
    }

    private List<Integer> generateRandomUniqueNumbers(int numberOfNumbers) {

        int MIN_ELEMENT_VALUE = 1;
        int MAX_ELEMENT_VALUE = 20;
        List<Integer> range = IntStream.rangeClosed(MIN_ELEMENT_VALUE, MAX_ELEMENT_VALUE).boxed().collect(Collectors.toList());
        return sampleSubsetUniformly(range, numberOfNumbers);
    }


    @Nullable
    @Override
    protected NliRootEntity generateRandomRootEntityForInitialState() {
        int MIN_LIST_LENGTH = 4;
        int MAX_LIST_LENGTH = 6;
        int listLength = sampleUniformly(MIN_LIST_LENGTH, MAX_LIST_LENGTH+1);
        SpecialList specialList = new SpecialList();
        generateRandomUniqueNumbers(listLength).stream()
                .map(ListElement::new)
                .forEach(specialList::addElement);
        return specialList;
    }

    @Nullable
    @Override
    protected MethodCall generateRandomFunctionCall(State initialState, StateVisualizer initialStateVisualizer,
                                                    StateVisualizer desiredStateVisualizer) {

        SpecialList specialList = (SpecialList) initialState.getRootEntity();

        if (nliMethod == ListNliMethod.REMOVE) {
            int numberOfNumbersToRemove = sampleUniformly(1, specialList.getElements().size()+1);
            List<ListElement> elements = sampleSubsetUniformly(specialList.getElements(), numberOfNumbersToRemove);
            elements.forEach(element->((ListStateVisualizer) initialStateVisualizer).emphasizeNumber(element.value));
            return new MethodCall(nliMethod.getMethodId(), initialState.getEntityId(specialList),
                    new NonPrimitiveArgument(initialState, elements));
        }

        Verify.verify((nliMethod == ListNliMethod.MOVE_TO_BEGINNING || nliMethod == ListNliMethod.MOVE_TO_END));
        ListElement arg = sampleUniformly(new ArrayList<>(specialList.getElements()));
        ((ListStateVisualizer) initialStateVisualizer).emphasizeNumber(arg.value);
        ((ListStateVisualizer) desiredStateVisualizer).emphasizeNumber(arg.value);
        return new MethodCall(nliMethod.getMethodId(), initialState.getEntityId(specialList),
                new NonPrimitiveArgument(initialState, arg));
    }

}
