package il.ac.technion.nlp.nli.dataset1.domains.calendar.hit;

import ofergivoli.olib.data_structures.set.SafeHashSet;
import ofergivoli.olib.data_structures.set.SafeSet;
import il.ac.technion.nlp.nli.core.dataset.DatasetDomains;
import il.ac.technion.nlp.nli.core.dataset.construction.hit_random_generation.HitRandomGenerator;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.method_call.MethodCall;
import il.ac.technion.nlp.nli.core.method_call.MethodId;
import il.ac.technion.nlp.nli.core.method_call.NonPrimitiveArgument;
import il.ac.technion.nlp.nli.core.method_call.PrimitiveArgument;
import il.ac.technion.nlp.nli.core.state.NliRootEntity;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.state.entities.Color;
import il.ac.technion.nlp.nli.dataset1.SettingsCommonForMultipleDomains;
import il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Calendar;
import il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Event;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarHitRandomGenerator extends HitRandomGenerator {

    private final SettingsCommonForMultipleDomains settingsCommonForMultipleDomains;

    private final TaskType taskType;
	public enum TaskType {
	    REMOVE_EVENT,
        SET_COLOR
    }

	public CalendarHitRandomGenerator(Random rand, TaskType taskType, DatasetDomains datasetDomains,
                                      SettingsCommonForMultipleDomains settingsCommonForMultipleDomains) {
		super(datasetDomains.getDomainByRootEntityClass(Calendar.class), rand, CalendarStateVisualizer::new);
        this.taskType = taskType;
        this.settingsCommonForMultipleDomains = settingsCommonForMultipleDomains;
    }

    private final ArrayList<Color> POSSIBLE_COLORS = HitRandomGenerator.createArrayList(
            Color.BLACK, Color.BLUE, Color.RED, Color.GREEN);


	@Nullable
	@Override
	protected NliRootEntity generateRandomRootEntityForInitialState() {

        int MIN_EVENTS_NUM = 4;
        int MAX_EVENTS_NUM = 8;
        ZonedDateTime CALENDAR_START_TIME = ZonedDateTime.of(2016, 11, 9, 0, 0, 0, 0, ZoneId.of("UTC"));
        int FIRST_HOUR_OF_SCHED = 8;
        int LAST_HOUR_OF_SCHED = 18;
        int MAX_ATENDEES_NUM = 2;

        ArrayList<String> POSSIBLE_TITLES = HitRandomGenerator.createArrayList(
                "meeting", "shopping", "working out", "jogging", "car wash", "fixing engine", "volleyball",
                "renting a car", "buying a piano", "getting a haircut", "buying office supplies");

        ArrayList<String> POSSIBLE_LOCATIONS = HitRandomGenerator.createArrayList(
                "Redwood", "Belmont", "Sunnyvale", "Los Altos", "San Jose", "Santa Clara");

        assert POSSIBLE_TITLES.size() >= MAX_EVENTS_NUM;


	    il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Calendar calendar = new il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Calendar();

	    List<ZonedDateTime> possibleEventStartTimes = new LinkedList<>();
        for (int hour = FIRST_HOUR_OF_SCHED; hour<= LAST_HOUR_OF_SCHED; hour++) {
	        possibleEventStartTimes.add(CALENDAR_START_TIME.plusHours(hour));
            possibleEventStartTimes.add(CALENDAR_START_TIME.plusDays(1).plusHours(hour));
        }

        int eventsNum = sampleUniformly(MIN_EVENTS_NUM, MAX_EVENTS_NUM +1);

        ArrayList<ZonedDateTime> startTimes = sampleSubsetUniformly(possibleEventStartTimes, eventsNum);
        ArrayList<String> titles = sampleSubsetUniformly(POSSIBLE_TITLES, eventsNum);

        for (int i=0; i<eventsNum; i++) {
            // for want to have a high density of some locations and colors and people:
            int attendeesNum = sampleUniformly(0,MAX_ATENDEES_NUM+1);
            SafeSet<String> attendees = new SafeHashSet<>();
            while (attendees.size()<attendeesNum)
                attendees.add(sampleOverFiniteGeometricDistribution(0.5,
                        settingsCommonForMultipleDomains.someFirstNames));
            String location = sampleOverFiniteGeometricDistribution(0.5, POSSIBLE_LOCATIONS);
            int colorInd = sampleOverFiniteGeometricDistribution(0.5, 0, POSSIBLE_COLORS.size());
            Color color = POSSIBLE_COLORS.get(colorInd);
            Event event = new Event(titles.get(i), startTimes.get(i), location, color, new LinkedList<>(attendees));
            calendar.events.add(event);
        }

	    return calendar;
	}

	@Nullable
	@Override
	protected MethodCall generateRandomFunctionCall(State initialState, StateVisualizer initialStateVisualizer,
                                                    StateVisualizer desiredStateVisualizer) {

        final int MIN_EVENT_NUM_TO_AFFECT = 1;
        final int MAX_EVENT_NUM_TO_AFFECT = 4;

        il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Calendar calendar = (il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Calendar) initialState.getRootEntity();
        MethodId nliMethod;
        switch(taskType) {
            case REMOVE_EVENT:
                nliMethod = new MethodId(il.ac.technion.nlp.nli.dataset1.domains.calendar.entities.Calendar.class, "removeEvents", Collection.class);
                break;
            case SET_COLOR:
                nliMethod = new MethodId(Calendar.class, "setEventColor", Collection.class, Color.class);
                break;
            default:
                throw new RuntimeException();
        }


        int eventNumToAffect = Math.min(calendar.events.size(),
                sampleUniformly(MIN_EVENT_NUM_TO_AFFECT, MAX_EVENT_NUM_TO_AFFECT+1));


        SafeSet<String> eventIds = sampleSubsetUniformly(calendar.events, eventNumToAffect).stream()
                .map(initialState::getEntityId)
                .collect(Collectors.toCollection(SafeHashSet::new));


        Color newColor = null;
        if (taskType == TaskType.SET_COLOR) {
            newColor = sampleUniformly(POSSIBLE_COLORS);

            // we don't want some of the event we emphasize to be already with the new color.
            Color finalNewColor = newColor; // just for Java's shenanigans.
            eventIds = eventIds.stream().filter(id->((Event)initialState.getEntityById(id)).color != finalNewColor)
                    .collect(Collectors.toCollection(SafeHashSet::new));
            if (eventIds.isEmpty())
                return null;

            eventIds.forEach(desiredStateVisualizer::emphasizeEntireEntity);
        }

        eventIds.forEach(initialStateVisualizer::emphasizeEntireEntity);


        if (taskType == TaskType.SET_COLOR) {
            return new MethodCall(nliMethod, initialState.getEntityId(calendar),
                    new NonPrimitiveArgument(eventIds), new PrimitiveArgument(newColor));
        }

        return new MethodCall(nliMethod, initialState.getEntityId(calendar), new NonPrimitiveArgument(eventIds));
	}

}
