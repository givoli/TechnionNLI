package il.ac.technion.nlp.nli.dataset1.domains.calendar.entities;

import ofergivoli.olib.io.log.Log;
import il.ac.technion.nlp.nli.core.EnableNli;
import il.ac.technion.nlp.nli.core.NliDescriptions;
import il.ac.technion.nlp.nli.core.method_call.InvalidNliMethodInvocation;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.entities.Color;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


public class Calendar implements NliRootEntity {
	
	private static final long serialVersionUID = -3374135115413443512L;

	public List<Event> events = new LinkedList<>();

	@EnableNli
    @NliDescriptions(descriptions = {"remove", "cancel"})
	public void removeEvents(Collection<Event> c) {
		c.forEach(event-> {
			if ( ! events.remove(event) ) {
				Log.warn("Event entity not in events list...");
				throw new InvalidNliMethodInvocation();
			}});
	}

	//TODO(mid): consider moving this to Event class.
	@EnableNli
    @NliDescriptions(descriptions = {"set","color"})
	public void setEventColor(Collection<Event> events, Color color) {
		events.forEach(event->event.color = color);
	}


}
