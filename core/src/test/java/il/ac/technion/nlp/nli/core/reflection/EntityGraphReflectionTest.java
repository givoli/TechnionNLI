package il.ac.technion.nlp.nli.core.reflection;

import il.ac.technion.nlp.nli.core.dataset.simple_test_domain.Group;
import il.ac.technion.nlp.nli.core.dataset.simple_test_domain.SimpleSocialNetwork;
import il.ac.technion.nlp.nli.core.dataset.simple_test_domain.User;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class EntityGraphReflectionTest {

    private SimpleSocialNetwork createSimpleSocialNetwork() {
        SimpleSocialNetwork result = new SimpleSocialNetwork();
        User user1 = new User();
        User user2 = new User();
        User user3 = new User();

        result.users.add(user1);
        result.users.add(user2);
        result.users.add(user3);

        user1.friends.add(user2);
        user1.friends.add(user3);
        user1.hobbies.add("1");
        user1.hobbies.add("2");

        Group group1 = new Group();
        group1.users.add(user1);
        group1.users.add(user2);
        Group group2 = new Group();
        group2.users.add(user3);

        result.groups.add(group1);
        result.groups.add(group2);


        return result;
    }

    @Test
    public void testEntityGraphEquals(){

        SimpleSocialNetwork ssn1 = createSimpleSocialNetwork();
        SimpleSocialNetwork ssn2 = createSimpleSocialNetwork();

        // verity order of non-primitive entities of relation matter.
        swapFirstAndSecondElements(ssn1.users);
        assertGraphsEquality(ssn1, ssn2, false);
        swapFirstAndSecondElements(ssn1.users); // swapping back to normal
        assertGraphsEquality(ssn1, ssn2, true);

        // verity order of primitive entities of relation matter.
        swapFirstAndSecondElements(ssn1.users.get(0).hobbies);
        assertGraphsEquality(ssn1, ssn2, false);
        swapFirstAndSecondElements(ssn1.users.get(0).hobbies);  // swapping back to normal
        assertGraphsEquality(ssn1, ssn2, true);
    }

    private void assertGraphsEquality(SimpleSocialNetwork ssn1, SimpleSocialNetwork ssn2, boolean equal) {
        assertTrue(EntityGraphReflection.entityGraphEquals(ssn1, ssn2) == equal);
        assertTrue(EntityGraphReflection.entityGraphEquals(ssn2, ssn1) == equal);
    }

    private <T> void swapFirstAndSecondElements(List<T> list) {
        ArrayList<T> result = new ArrayList<>(list);
        T obj = result.remove(1);
        result.add(0,obj);
        list.clear();
        list.addAll(result);
    }
}