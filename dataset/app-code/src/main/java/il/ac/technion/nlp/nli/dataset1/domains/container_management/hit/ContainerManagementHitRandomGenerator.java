package il.ac.technion.nlp.nli.dataset1.domains.container_management.hit;

import ofergivoli.olib.data_structures.set.SafeHashSet;
import ofergivoli.olib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.dataset1.domains.container_management.entities.ContentState;
import il.ac.technion.nlp.nli.dataset1.domains.container_management.entities.ShippingContainer;
import il.ac.technion.nlp.nli.core.method_call.NonPrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitRandomGenerator;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.dataset1.domains.container_management.entities.ContainerManagementSystem;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ContainerManagementHitRandomGenerator extends HitRandomGenerator
{

    private final ContainerManagementNliMethod nliMethod;
    static final int MAX_CONTAINER_LENGTH = 4;

    public ContainerManagementHitRandomGenerator(Random rand, ContainerManagementNliMethod nliMethod,
                                                 DatasetDomains datasetDomains) {

        super(datasetDomains.getDomainByRootEntityClass(ContainerManagementSystem.class), rand,
                ContainerManagementStateVisualizer::new);
        this.nliMethod = nliMethod;
    }

    @Nullable
    @Override
    protected NliRootEntity generateRandomRootEntityForInitialState() {

        final int MIN_CONTAINERS_NUM = 3;
        final int MAX_CONTAINERS_NUM = 7;

        ContainerManagementSystem containerManagement = new ContainerManagementSystem();

        int containersNum = sampleUniformly(MIN_CONTAINERS_NUM, MAX_CONTAINERS_NUM + 1);

        containerManagement.containers = generateRandomContainers(containersNum);

        return containerManagement;
    }

    private List<ShippingContainer> generateRandomContainers(int containersNum) {

        List<ShippingContainer> result = new LinkedList<>();

        for (int i=0; i<containersNum; i++) {
            int length = sampleUniformly(1, MAX_CONTAINER_LENGTH +1);
            result.add(new ShippingContainer(length, sampleUniformly(ContentState.class)));
        }
        return result;
    }



    @Nullable
    @Override
    protected MethodCall generateRandomFunctionCall(State initialState, StateVisualizer initialStateVisualizer,
                                                    StateVisualizer desiredStateVisualizer) {

        ContainerManagementSystem containerManagement = (ContainerManagementSystem) initialState.getRootEntity();

        int MAX_CONTAINERS_TO_EFFECT = 4;

        int containersNumToEffect = Math.min(containerManagement.containers.size(),
                sampleUniformly(1, MAX_CONTAINERS_TO_EFFECT+1));

        SafeSet<String> ids = sampleSubsetUniformly(containerManagement.containers, containersNumToEffect).stream()
                .map(initialState::getEntityId)
                .collect(Collectors.toCollection(SafeHashSet::new));

        ((ContainerManagementStateVisualizer) desiredStateVisualizer).setContainerPositionsByState(initialState);
        return new MethodCall(nliMethod.getMethodId(), initialState.getEntityId(containerManagement),
                new NonPrimitiveArgument(ids));
    }


}
