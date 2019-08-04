package il.ac.technion.nlp.nli.core.dataset.construction;

import com.ofergivoli.ojavalib.data_structures.set.SafeHashSet;
import com.ofergivoli.ojavalib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.core.dataset.simple_test_domain.SimpleSocialNetwork;
import il.ac.technion.nlp.nli.core.dataset.simple_test_domain.User;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.method_call.NonPrimitiveArgument;
import il.ac.technion.nlp.nli.core.method_call.PrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.simple_test_domain.Group;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Collection;

import static org.junit.Assert.*;




/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class HitTest {

    private User user1;
    private User user2;

    private SimpleSocialNetwork simpleSocialNetwork;

    @Test
    public void testIsQueryableByPrimitiveRelationsWithSingleNonPrimitiveEntity() throws Exception {

        // only one non-primitive entity (one time in args and one time not)
        simpleSocialNetwork.users.add(user1);

        State initialState = createStateFromCurrentSimpleSocialNetwork();

        for (int i=0; i<2; i++) {
            MethodId method = new MethodId(User.class, "setAgeInTwoCollections",
                    Collection.class, Integer.TYPE, Collection.class, Integer.TYPE);
            NonPrimitiveArgument usersInArgs =  i==0 ?
                    new NonPrimitiveArgument() :
                    new NonPrimitiveArgument(initialState.getEntityId(user1));
            MethodCall fc = new MethodCall(method, initialState.getEntityId(user1),
                    usersInArgs,
                    new PrimitiveArgument(3),
                    usersInArgs,
                    new PrimitiveArgument(3));
            assertTrue(generateHit(initialState, fc).isQueryableByPrimitiveRelations());
        }
    }

    @Test
    public void testIsQueryableByPrimitiveRelationsWithTwoPrimitiveEntities() throws Exception {

        for (int j=0; j<5; j++) { // checking multiple settings

            resetSimpleSocialNetworkAndUpdateInitialState();
            simpleSocialNetwork.users.add(user1);
            simpleSocialNetwork.users.add(user2);

            boolean user2_interfere_user1;
            if (j<2) {
                //We change triples in a way that keeps user2 interfering.
                user2_interfere_user1 = true;
                if (j==0)
                    user1.friends.add(user2); // Adding triple of a non-primitive relation should make no difference.
                user1.hobbies.add("biking");
                user2.hobbies.add("biking");
            } else {
                // We change triples in way that makes user2 not interfere when querying user1.
                user2_interfere_user1 = false;
                if (j==2)
                    user1.hobbies.add("biking");
                else if (j==3) {
                    user2.hobbies.add("biking");
                } else {
                    assert j==4;
                    user1.age++;
                }
            }

            State initialState = createStateFromCurrentSimpleSocialNetwork();

            for (int usersInArgNum = 0; usersInArgNum <= 2; usersInArgNum++) {
                // We path as arg either {} or {user1} or {user1,user2}.

                NonPrimitiveArgument emptyArg = new NonPrimitiveArgument();
                SafeSet<String> usersInArg = new SafeHashSet<>();
                if (usersInArgNum>0)
                    usersInArg.add(initialState.getEntityId(user1));
                if (usersInArgNum>1)
                    usersInArg.add(initialState.getEntityId(user2));
                NonPrimitiveArgument otherArg = new NonPrimitiveArgument(usersInArg);

                for (int emptyArgInd=0; emptyArgInd<2; emptyArgInd++) {
                    // In each iteration the users (if any) appear as a different user-collection argument and the other
                    // user-collection argument is empty.

                    MethodId method = new MethodId(User.class, "setAgeInTwoCollections",
                            Collection.class, Integer.TYPE, Collection.class, Integer.TYPE);
                    NonPrimitiveArgument nonPrimitiveArg0 =  emptyArgInd == 0 ? emptyArg : otherArg;
                    NonPrimitiveArgument nonPrimitiveArg1 =  emptyArgInd == 1 ? emptyArg : otherArg;
                    MethodCall fc = new MethodCall(method, initialState.getEntityId(user1),
                            nonPrimitiveArg0,
                            new PrimitiveArgument(3),
                            nonPrimitiveArg1,
                            new PrimitiveArgument(3));
                    boolean expectedRes =
                            usersInArgNum==0 || // trivially queryable (no non-primitive entities are passed as arg)
                            usersInArgNum==2 || // trivially queryable (all non-primitive entities are passed as arg)
                            !user2_interfere_user1;
                    assertEquals(expectedRes, generateHit(initialState,fc).isQueryableByPrimitiveRelations());
                }

            }
        }

    }

    @Test
    public void testContainsArgQueryableByComparisonOperation() throws Exception {

        simpleSocialNetwork.users.add(user1);
        simpleSocialNetwork.users.add(user2);
        user1.age = 1;
        user2.age = 2;
        Group group = new Group();
        simpleSocialNetwork.groups.add(group); // just to have another object without a triple from the relevant relation.

        State initialState = createStateFromCurrentSimpleSocialNetwork();

        MethodId method = new MethodId(User.class, "setAgeInTwoCollections",
                Collection.class, Integer.TYPE, Collection.class, Integer.TYPE);

        // Less than two entities per arg.
        MethodCall fc = new MethodCall(method, initialState.getEntityId(user1),
                new NonPrimitiveArgument(initialState.getEntityId(user1)),
                new PrimitiveArgument(3),
                new NonPrimitiveArgument(initialState.getEntityId(user2)),
                new PrimitiveArgument(3));
        assertFalse(generateHit(initialState, fc).containsArgQueryableByComparisonOperation());


        // no entity exists which is not in arg and has a triple for the relevant relation (User's age).
        fc = new MethodCall(method, initialState.getEntityId(user1),
                new NonPrimitiveArgument(),
                new PrimitiveArgument(3),
                new NonPrimitiveArgument(initialState.getEntityId(user1), initialState.getEntityId(user2)),
                new PrimitiveArgument(3));
        assertFalse(generateHit(initialState,fc).containsArgQueryableByComparisonOperation());


        assertContainsArgQueryableByComparisonOperationReturnsExpectedResult(1,1,2,true);
        assertContainsArgQueryableByComparisonOperationReturnsExpectedResult(1,1,0,true);
        assertContainsArgQueryableByComparisonOperationReturnsExpectedResult(1,2,3,true);
        assertContainsArgQueryableByComparisonOperationReturnsExpectedResult(1,2,0,true);

        assertContainsArgQueryableByComparisonOperationReturnsExpectedResult(1,2,1,false);
        assertContainsArgQueryableByComparisonOperationReturnsExpectedResult(1,2,2,false);
        assertContainsArgQueryableByComparisonOperationReturnsExpectedResult(1,3,2,false);
    }


    /**
     * Tests the {@link Hit#containsArgQueryableByComparisonOperation()} in a scenario where an argument contains two
     * users and there is a third user not in the arg.
     */
    private void assertContainsArgQueryableByComparisonOperationReturnsExpectedResult(
            @SuppressWarnings("SameParameterValue") int ageOfUser1_inArg, int ageOfUser2_inArg, int ageOfUserNotInArg,
            boolean expectedRes) {

        resetSimpleSocialNetworkAndUpdateInitialState();
        user1.age = ageOfUser1_inArg;
        user2.age = ageOfUser2_inArg;
        simpleSocialNetwork.users.add(user1);
        simpleSocialNetwork.users.add(user2);

        User userNotInArg = new User();
        userNotInArg.age = ageOfUserNotInArg;
        simpleSocialNetwork.users.add(userNotInArg);

        State initialState = createStateFromCurrentSimpleSocialNetwork();

        MethodId method = new MethodId(User.class, "setAgeInTwoCollections",
                Collection.class, Integer.TYPE, Collection.class, Integer.TYPE);

        MethodCall fc = new MethodCall(method, initialState.getEntityId(user1),
                new NonPrimitiveArgument(initialState.getEntityId(user1), initialState.getEntityId(user2)),
                new PrimitiveArgument(3),
                new NonPrimitiveArgument(),
                new PrimitiveArgument(3));
        assertEquals(expectedRes, generateHit(initialState,fc).containsArgQueryableByComparisonOperation());
    }




    @Test
    public void testContainsArgQueryableBySuperlativeOperation() throws Exception {

        simpleSocialNetwork.users.add(user1);
        simpleSocialNetwork.users.add(user2);
        user1.age = 1;
        user2.age = 2;
        Group group = new Group();
        simpleSocialNetwork.groups.add(group); // just to have another object without a triple from the relevant relation.

        State initialState = createStateFromCurrentSimpleSocialNetwork();

        MethodId method = new MethodId(User.class, "setAgeInTwoCollections",
                Collection.class, Integer.TYPE, Collection.class, Integer.TYPE);

        // empty arg.
        MethodCall fc = new MethodCall(method, initialState.getEntityId(user1),
                new NonPrimitiveArgument(),
                new PrimitiveArgument(3),
                new NonPrimitiveArgument(),
                new PrimitiveArgument(3));
        assertFalse(generateHit(initialState, fc).containsArgQueryableBySuperlativeOperation());


        // no entity exists which is not in arg and has a triple for the relevant relation (User's age).
        fc = new MethodCall(method, initialState.getEntityId(user1),
                new NonPrimitiveArgument(),
                new PrimitiveArgument(3),
                new NonPrimitiveArgument(initialState.getEntityId(user1), initialState.getEntityId(user2)),
                new PrimitiveArgument(3));
        assertFalse(generateHit(initialState,fc).containsArgQueryableBySuperlativeOperation());


        // simple positive-result scenarios:
        fc = new MethodCall(method, initialState.getEntityId(user1),
                new NonPrimitiveArgument(initialState.getEntityId(user1)),
                new PrimitiveArgument(3),
                new NonPrimitiveArgument(),
                new PrimitiveArgument(3));
        assertTrue(generateHit(initialState,fc).containsArgQueryableBySuperlativeOperation());
        fc = new MethodCall(method, initialState.getEntityId(user1),
                new NonPrimitiveArgument(),
                new PrimitiveArgument(3),
                new NonPrimitiveArgument(initialState.getEntityId(user2)),
                new PrimitiveArgument(3));
        assertTrue(generateHit(initialState,fc).containsArgQueryableBySuperlativeOperation());



        assertContainsArgQueryableBySuperlativeOperationReturnsExpectedResult(2,3,1, 1,false);
        assertContainsArgQueryableBySuperlativeOperationReturnsExpectedResult(2,2,1, 1,true);

        assertContainsArgQueryableBySuperlativeOperationReturnsExpectedResult(2,2,3, 3,true);
        assertContainsArgQueryableBySuperlativeOperationReturnsExpectedResult(2,2,3, 4,true);

        assertContainsArgQueryableBySuperlativeOperationReturnsExpectedResult(2,2,1, 3,false);
    }

    private void assertContainsArgQueryableBySuperlativeOperationReturnsExpectedResult(
            @SuppressWarnings("SameParameterValue") int ageOfUser1_inArg, int ageOfUser2_inArg, int ageOfUser1_NotInArg,
            int ageOfUser2_NotInArg, boolean expectedRes) {

        resetSimpleSocialNetworkAndUpdateInitialState();
        user1.age = ageOfUser1_inArg;
        user2.age = ageOfUser2_inArg;
        simpleSocialNetwork.users.add(user1);
        simpleSocialNetwork.users.add(user2);

        User User1_NotInArg = new User();
        User User2_NotInArg = new User();
        User1_NotInArg.age = ageOfUser1_NotInArg;
        User2_NotInArg.age = ageOfUser2_NotInArg;
        simpleSocialNetwork.users.add(User1_NotInArg);
        simpleSocialNetwork.users.add(User2_NotInArg);

        State initialState = createStateFromCurrentSimpleSocialNetwork();

        MethodId method = new MethodId(User.class, "setAgeInTwoCollections",
                Collection.class, Integer.TYPE, Collection.class, Integer.TYPE);

        MethodCall fc = new MethodCall(method, initialState.getEntityId(user1),
                new NonPrimitiveArgument(initialState.getEntityId(user1), initialState.getEntityId(user2)),
                new PrimitiveArgument(3),
                new NonPrimitiveArgument(),
                new PrimitiveArgument(3));
        assertEquals(expectedRes, generateHit(initialState,fc).containsArgQueryableBySuperlativeOperation());
    }


    @Before
    public void initialize() {
        resetSimpleSocialNetworkAndUpdateInitialState();
    }

    private void resetSimpleSocialNetworkAndUpdateInitialState() {
        user1 = new User();
        user2 = new User();
        simpleSocialNetwork = new SimpleSocialNetwork();
    }


    private State createStateFromCurrentSimpleSocialNetwork() {
        return new State(SimpleSocialNetwork.domain, simpleSocialNetwork, true);
    }

    private Hit generateHit(State initialState, MethodCall fc) {
        return new Hit(SimpleSocialNetwork.domain, initialState, fc.invokeOnDeepCopyOfState(initialState), fc,
                new DummyVisualizer(), new DummyVisualizer(), "", ZonedDateTime.now());
    }


    private static class DummyVisualizer extends StateVisualizer {
        private static final long serialVersionUID = -6246222939193237330L;
        @Override
        public HtmlString getVisualRepresentation(State state) {
            return null;
        }
    }
}