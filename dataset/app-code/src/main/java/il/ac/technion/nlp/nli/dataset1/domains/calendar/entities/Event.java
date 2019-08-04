package il.ac.technion.nlp.nli.dataset1.domains.calendar.entities;

import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.entities.Color;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

public class Event implements NliEntity {


	private static final long serialVersionUID = -8506914020791893380L;

	public String title;

    public ZonedDateTime startTime;

	@Nullable
    public String location;

    public Color color;

    public List<String> attendees = new LinkedList<>();


	public Event(String title, ZonedDateTime startTime, @Nullable String location, Color color,
                 List<String> attendees) {
		this.title = title;
		this.startTime = startTime;
		this.location = location;
		this.color = color;
		this.attendees = attendees;
	}

	public Event(String title, ZonedDateTime startTime) {
		this(title, startTime, null, Color.BLACK, new LinkedList<>());
	}


}
