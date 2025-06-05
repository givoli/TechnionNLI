package il.ac.technion.nlp.nli.dataset1.domains.lighting_control.hit;

import ofergivoli.olib.data_structures.set.SafeHashSet;
import ofergivoli.olib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities.LightMode;
import il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities.LightingControlSystem;
import il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities.Room;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.method_call.NonPrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitRandomGenerator;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class LightingControlHitRandomGenerator extends HitRandomGenerator {

    /**
     * see constructor.
     */
    private final LightMode newLightMode;

    private final ArrayList<String> ROOM_NAMES = HitRandomGenerator.createArrayList(
            "bathroom", "hall", "bedroom", "living room", "kitchen", "dining room", "family room");


    /**
     * @param newLightMode defines whether the HIT will be about turning lights on or off.
     */
    public LightingControlHitRandomGenerator(Random rand, LightMode newLightMode, DatasetDomains datasetDomains) {
        super(datasetDomains.getDomainByRootEntityClass(LightingControlSystem.class), rand,
                LightingControlStateVisualizer::new);
        this.newLightMode = newLightMode;
    }


    @Override
    protected @Nullable
    NliRootEntity generateRandomRootEntityForInitialState() {

        int MIN_FLOORS_NUM = 2;
        final int MAX_FLOORS_NUM = 4;
        final int MIN_ROOMS_PER_FLOOR = 2;
        final int MAX_ROOMS_PER_FLOOR = 3;


        LightingControlSystem result = new LightingControlSystem();

        int numOfFloors = sampleUniformly(MIN_FLOORS_NUM, MAX_FLOORS_NUM+1);

        for (int floorNum=1; floorNum<=numOfFloors; floorNum++) {
            int roomsNum = sampleUniformly(MIN_ROOMS_PER_FLOOR, MAX_ROOMS_PER_FLOOR+1);

            // we don't want the visualization to show a floor with multiple consecutive bathrooms...
            SafeSet<String> roomNames = new SafeHashSet<>();
            while (roomNames.size() < roomsNum)
                roomNames.add(sampleOverFiniteGeometricDistribution(0.3, ROOM_NAMES));

            for (String name : roomNames)
                    result.rooms.add(new Room(name, sampleUniformly(LightMode.class), floorNum));
        }

        return result;
    }




    @Override
    protected @Nullable
    MethodCall generateRandomFunctionCall(State initialState, StateVisualizer initialStateVisualizer,
                                          StateVisualizer desiredStateVisualizer) {

        int MIN_ROOMS_NUM = 1;
        int MAX_ROOMS_NUM = 5;

        MethodId nliMethod = newLightMode==LightMode.ON ?
                new MethodId(LightingControlSystem.class, "turnLightOn", Collection.class) :
                new MethodId(LightingControlSystem.class, "turnLightOff", Collection.class);

        LightingControlSystem lcs = (LightingControlSystem) initialState.getRootEntity();
        List<Room> relevantRooms = lcs.rooms.stream()
                .filter(room->room.lightMode!=newLightMode)
                .collect(Collectors.toList());
        int roomsModifyingNum = Math.min(relevantRooms.size(), sampleUniformly(MIN_ROOMS_NUM, MAX_ROOMS_NUM+1));

        if (roomsModifyingNum == 0)
            return null;

        SafeSet<String> roomIds = sampleSubsetUniformly(relevantRooms, roomsModifyingNum).stream()
                .map(initialState::getEntityId)
                .collect(Collectors.toCollection(SafeHashSet::new));

        return new MethodCall(nliMethod, initialState.getEntityId(lcs), new NonPrimitiveArgument(roomIds));
    }

}
