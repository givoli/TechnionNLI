package il.ac.technion.nlp.nli.parser.features.denotation.state_delta;

import edu.stanford.nlp.sempre.NameValue;
import edu.stanford.nlp.sempre.NumberValue;
import edu.stanford.nlp.sempre.Value;
import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.parser.InstructionKnowledgeGraph;
import il.ac.technion.nlp.nli.parser.features.denotation.StateDelta;
import il.ac.technion.nlp.nli.parser.kb.KbTriple;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
@SuppressWarnings("serial")
public class StateDeltaTest {


    private static class DummyEntity implements NliEntity {
        public int x;
        public List<Integer> list = new LinkedList<>();

        public DummyEntity(int x) {
            this.x = x;
        }
    }

    private static class DummyRoot implements NliRootEntity {
        List<DummyEntity> entites = new LinkedList<>();
    }

    /**
     * Also test the logic of {@link StateDelta#create(InstructionKnowledgeGraph, InstructionKnowledgeGraph, boolean)}.
     */
    @Test
    public void testFindTriplesInInitialStateThatChange() throws NoSuchFieldException {

        DummyRoot root = new DummyRoot();
        for (int i=0; i<=5; i++) {
            DummyEntity e = new DummyEntity(i);
            e.list.add(100+i);
            e.list.add(1000+i);
            root.entites.add(e);
        }
        State state1 = new State(new Domain("", DummyRoot.class), root, true);
        State state2 = state1.deepCopy();

        DummyRoot rootOfState2 = (DummyRoot) state2.getRootEntity();


        rootOfState2.entites.get(3).x = 999;
        rootOfState2.entites.get(4).x = 3;
        rootOfState2.entites.get(5).list.remove(0); // the value 105 is removed.
        rootOfState2.entites.get(5).list.add(998);


        state2.updateStateFollowingEntityGraphModifications();


        InstructionKnowledgeGraph graph1 = new InstructionKnowledgeGraph(state1, true);
        InstructionKnowledgeGraph graph2 = new InstructionKnowledgeGraph(state2, true);

        StateDelta delta = StateDelta.create(graph1, graph2, true);


        assertEquals(new HashSet<>(
                Arrays.asList(createTriple(state1, graph1, 3, 3, "x"),
                              createTriple(state1, graph1, 4, 4, "x"),
                              createTriple(state1, graph1, 5, 105, "list"))),
                new HashSet<>(delta.findTriplesInInitialStateThatChange()));
    }


    private KbTriple createTriple(State state, InstructionKnowledgeGraph graph, int entityIndex,
                                  int entityValue, String fieldName) throws NoSuchFieldException {

        NameValue firstArg = graph.nameValuesManager.createNameValueRepresentingNliEntity(state,
                ((DummyRoot) state.getRootEntity()).entites.get(entityIndex));

        NameValue relation = graph.nameValuesManager.createNameValueRepresentingRelationField(
                DummyEntity.class.getField(fieldName));

        Value secondArg = new NumberValue(entityValue);

        return new KbTriple(firstArg, relation, secondArg);
    }

}