package il.ac.technion.nlp.nli.dataset1.domains.lighting_control.hit;

import il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities.LightMode;
import il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities.LightingControlSystem;
import il.ac.technion.nlp.nli.dataset1.domains.lighting_control.entities.Room;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class LightingControlStateVisualizer extends StateVisualizer {

    private static final long serialVersionUID = 687073389352325771L;


    @Override
    public HtmlString getVisualRepresentation(State state) {
        StringBuffer sb = new StringBuffer();

        sb.append("<table style=\"font-size: 2.0em;border-collapse:collapse;text-align:center;\">");

        // We want to show floor 2 on top of floor 1 etc. (like a real building...)
        LightingControlSystem root = (LightingControlSystem) state.getRootEntity();

        int floorsNum = root.rooms.stream()
                .map(room->room.floor)
                .collect(Collectors.toSet())
                .size();
        for (int i = floorsNum; i>=1; i--) {
            visualizeFloor(root, sb, i);
        }

        sb.append("</table>");

        return new HtmlString(sb.toString());
    }

    private void visualizeFloor(LightingControlSystem root, StringBuffer sb, int floorNum) {

        sb.append("<tr><td style=\"background-color: #00FFFF;border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\">")
                .append("floor ").append(floorNum).append("</td>");

        List<Room> l =  root.rooms.stream()
                .filter(r -> r.floor == floorNum)
                .collect(Collectors.toList());

        l.sort(Comparator.comparing(o -> o.roomName));

        l.forEach(r -> visualizeRoom(sb, r));

        sb.append("</tr>");
    }

    private void visualizeRoom(StringBuffer sb, Room r) {
        final String lightsOffColor = "#bfbfbf";
        final String lightsOnColor = "#ff741a";

        String colorStr;
        if (r.getLightMode() == LightMode.ON)
            colorStr = lightsOnColor;
        else if (r.getLightMode() == LightMode.OFF)
            colorStr = lightsOffColor;
        else
            throw new Error();

        sb.append("<td style=\"background-color: ")
                .append(colorStr)
                .append(";border-style: solid;border-width: 0.1em;vertical-align:center;padding:0.12em\"><span style=\"font-size: 0.6em\">")
                .append(r.roomName)
                .append("</span></td>");
    }

}
