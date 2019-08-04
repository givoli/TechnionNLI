package il.ac.technion.nlp.nli.dataset1.domains.calendar.hit;

import com.ofergivoli.ojavalib.time.TemporalFormat;
import il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Calendar;
import il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Event;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextColor;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextFormatModification;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextSizeModification;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class CalendarStateVisualizer extends StateVisualizer {

	private static final long serialVersionUID = 3300102420302383399L;




	@Override
	public HtmlString getVisualRepresentation(State state) {

		Calendar calendar = (Calendar) state.getRootEntity();


		StringBuilder sb = new StringBuilder();

		sb.append("<div style=\"text-align: left;\">");

		List<Event> eventsList = new LinkedList<>(calendar.events);

		eventsList.sort(Comparator.comparing(e -> e.startTime));

		if (!eventsList.isEmpty()) {
			ZonedDateTime dateOfLastEvent = null;
			for (Event event : eventsList) {

				if (dateOfLastEvent == null || event.startTime.toLocalDate().isAfter(dateOfLastEvent.toLocalDate())) {
					// We need to visualize the new date:
					if (dateOfLastEvent != null)
						sb.append("<br>");
					writeVisualRepresentationOfDate(sb, event.startTime);
				}
				writeVisualRepresentationOfEvent(sb,state,event);
				dateOfLastEvent = event.startTime;
			}
		}
		sb.append("</div>");
		return new HtmlString(sb.toString());
	}



	/**
	 * Writes the "date" line between events in different dates.
	 */
	private void writeVisualRepresentationOfDate(StringBuilder sb, ZonedDateTime date) {
		TextFormatModification format = new TextFormatModification();
		format.setBold(true);
		format.setSizeInCssEm(TextSizeModification.BIGGER);
		format.setColor(TextColor.SADDLEBROWN);

		String str = new HtmlString(TemporalFormat.temporalToStringUS(date,"EEEE, d MMM") + ":", format,
				true).getString();

		sb.append(str);
		sb.append("<br>");
	}


	private void writeVisualRepresentationOfEvent(StringBuilder sb, State state, Event event) {

		TextFormatModification formatForEntireEvent = new TextFormatModification();

		if (isEntityEntirelyEmphasized(state.getEntityId(event)))
			formatForEntireEvent.setUnderline(true);

		formatForEntireEvent.setColor(TextColor.fromColorEntity(event.color));

		StringBuilder eventSb = new StringBuilder();

		String startDateStr = TemporalFormat.temporalToStringUS(event.startTime,"HH:mm");

		eventSb.append(startDateStr).append(" &nbsp");

		eventSb.append(event.title).append(" ");

		if (event.location != null)
			eventSb.append("(at: ").append(event.location).append(") &nbsp");

		if (!event.attendees.isEmpty())
		{
			String attendeeStr = event.attendees.size() == 1 ? "  Attendee: " : "  Attendees: ";
			HtmlString html = new HtmlString(attendeeStr, new TextFormatModification().setBold(true), false);
			eventSb.append(html.getString());
			Iterator<String> it = event.attendees.iterator();
			while (it.hasNext()) {
				eventSb.append(it.next());
				if (it.hasNext())
					eventSb.append(", ");
			}
		}

		sb.append(new HtmlString(eventSb.toString(),formatForEntireEvent,false).getString());
		sb.append("<br>");
	}


}

