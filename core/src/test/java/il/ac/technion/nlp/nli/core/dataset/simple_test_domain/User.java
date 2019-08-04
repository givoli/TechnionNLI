package il.ac.technion.nlp.nli.core.dataset.simple_test_domain;

import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.state.NliEntity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class User implements NliEntity {

    private static final long serialVersionUID = -4117150756984253787L;

    public List<String> hobbies = new LinkedList<>();
    public List<User> friends = new LinkedList<>();
    public int age = 0;

    /**
     * Don't look for real-life motivation here :)
     */
    @EnableNli
    public void setAgeInTwoCollections(Collection<User> c1, int age1,
                                       Collection<User> c2, int age2) {
        c1.forEach(u->u.age = age1);
        c2.forEach(u->u.age = age2);
    }

    @EnableNli
    public void addFriend(User u) {
        friends.add(u);
    }

    @EnableNli
    public void setAge(int age) {
        this.age = age;
    }
}
