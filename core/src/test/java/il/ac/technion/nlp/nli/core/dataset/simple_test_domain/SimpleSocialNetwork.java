package il.ac.technion.nlp.nli.core.dataset.simple_test_domain;

import il.ac.technion.nlp.nli.core.dataset.Domain;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class SimpleSocialNetwork implements NliRootEntity {

    private static final long serialVersionUID = -3394576914362725326L;

    public static Domain domain = new Domain("test_domain__social_network", SimpleSocialNetwork.class);

    public List<User> users = new LinkedList<>();
    public List<Group> groups = new LinkedList<>();

}
