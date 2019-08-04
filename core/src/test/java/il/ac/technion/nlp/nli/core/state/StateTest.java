package il.ac.technion.nlp.nli.core.state;

import il.ac.technion.nlp.nli.core.method_call.*;
import il.ac.technion.nlp.nli.core.dataset.simple_test_domain.SimpleSocialNetwork;
import il.ac.technion.nlp.nli.core.dataset.simple_test_domain.User;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class StateTest {

    @Test
    public void testMethodCallInvocation() throws Exception {

        User user1;
        User user2;

        user1 = new User();
        user2 = new User();
        State state1 = new State(SimpleSocialNetwork.domain, createSimpleSocialNetwork(user1, user2), true);
        assertExpectedEqualityOfTwoStates(state1,state1, true);

        //adding non-primitive triple
        MethodId methodId = new MethodId(User.class, "addFriend", User.class);
        Argument arg = new NonPrimitiveArgument(state1.getEntityId(user2));
        MethodCall methodCall = new MethodCall(methodId, state1.getEntityId(user1), arg);
        State state2 = methodCall.invokeOnDeepCopyOfState(state1);
        State state2_copy = methodCall.invokeOnDeepCopyOfState(state1);
        assertExpectedEqualityOfTwoStates(state2,state2_copy, true); // just a sanity check.
        assertExpectedEqualityOfTwoStates(state1,state2, false);

        //changing a primitive triple
        methodId = new MethodId(User.class, "setAge", int.class);
        arg = new PrimitiveArgument(user1.age+1);
        methodCall = new MethodCall(methodId, state1.getEntityId(user1),arg);
        State state3 = methodCall.invokeOnDeepCopyOfState(state1);
        State state3_copy = methodCall.invokeOnDeepCopyOfState(state1);
        assertExpectedEqualityOfTwoStates(state3,state3_copy, true); // just a sanity check.
        assertExpectedEqualityOfTwoStates(state1,state3, false);
    }

    private void assertExpectedEqualityOfTwoStates(State s1, State s2, boolean identical) {
        assertTrue(s1.entityGraphsEqual(s2) == identical);
        assertTrue(s2.entityGraphsEqual(s1) == identical);
    }

    @NotNull
    private SimpleSocialNetwork createSimpleSocialNetwork(User... users) {
        SimpleSocialNetwork simpleSocialNetwork = new SimpleSocialNetwork();
        Collections.addAll(simpleSocialNetwork.users, users);
        return simpleSocialNetwork;
    }

}