package il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities;

import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.NliDescriptions;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class LightingControlSystem implements NliRootEntity {


    private static final long serialVersionUID = -4869440484917040177L;

    public List<Room> rooms = new LinkedList<>();

    @EnableNli
    @NliDescriptions(descriptions = {"turn on"})
    public void turnLightOn(Collection<Room> rooms) {
        rooms.forEach(r->r.lightMode = LightMode.ON);
    }

    @EnableNli
    @NliDescriptions(descriptions = {"turn off"})
    public void turnLightOff(Collection<Room> rooms) {
        rooms.forEach(r->r.lightMode = LightMode.OFF);
    }

}
