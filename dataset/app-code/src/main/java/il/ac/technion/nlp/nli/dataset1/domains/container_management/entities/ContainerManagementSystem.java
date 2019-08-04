package il.ac.technion.nlp.nli.dataset1.domains.container_management.entities;

import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.NliDescriptions;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ContainerManagementSystem implements NliRootEntity {

    private static final long serialVersionUID = 4401710955123646220L;
    public List<ShippingContainer> containers = new LinkedList<>();

    @NliDescriptions(descriptions = {"load", "fill"})
    @EnableNli
    public void loadContainers(Collection<ShippingContainer> c) {
        c.forEach(container->container.contentState = ContentState.LOADED);
    }

    @NliDescriptions(descriptions = {"unload", "clear"})
    @EnableNli
    public void unloadContainers(Collection<ShippingContainer> c) {
        c.forEach(container->container.contentState = ContentState.UNLOADED);
    }

    @NliDescriptions(descriptions = {"remove"})
    @EnableNli
    public void removeContainers(Collection<ShippingContainer> c) {
        containers.removeAll(c);
    }
}
