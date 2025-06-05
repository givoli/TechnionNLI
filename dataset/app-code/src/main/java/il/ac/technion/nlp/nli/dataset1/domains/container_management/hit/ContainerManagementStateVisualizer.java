package il.ac.technion.nlp.nli.dataset1.domains.container_management.hit;

import ofergivoli.olib.data_structures.map.SafeHashMap;
import ofergivoli.olib.data_structures.map.SafeMap;
import il.ac.technion.nlp.nli.dataset1.domains.container_management.entities.ShippingContainer;
import il.ac.technion.nlp.nli.dataset1.domains.container_management.entities.ContainerManagementSystem;
import il.ac.technion.nlp.nli.dataset1.domains.container_management.entities.ContentState;
import il.ac.technion.nlp.nli.core.state.State;
import il.ac.technion.nlp.nli.core.dataset.visualization.StateVisualizer;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextColor;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextFormatModification;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class ContainerManagementStateVisualizer extends StateVisualizer {

    /**
     * @param stateWithOriginalContainerPositions Doesn't have to be the state that will be visualized, but must contain
     *                                            all the {@link ShippingContainer} that will be visualized.
     *                                            See {@link #containerPositionToId}.
     *                                            A reference to the argument is not saved.
     */
    public void setContainerPositionsByState(State stateWithOriginalContainerPositions) {
        containerPositionToId = generateContainerPositionToIdMap(stateWithOriginalContainerPositions);
    }

    private static SafeMap<Integer, String> generateContainerPositionToIdMap(State state)
    {
        SafeMap<Integer, String> containerPositionToId = new SafeHashMap<>();
        List<ShippingContainer> containers =
                ((ContainerManagementSystem) state.getRootEntity()).containers;
        containers.forEach(c-> containerPositionToId.put(containers.indexOf(c),state.getEntityId(c)));
        return containerPositionToId;
    }

    private static final long serialVersionUID = 7774549385090549119L;

    /**
     * A map from the an index in {@link ContainerManagementSystem#containers} of the state sent to
     * {@link #setContainerPositionsByState(State)}, and the the id of the {@link ShippingContainer}.
     * When not null, the position of the containers in the visualization is determined by this map. Use this map to make
     * containers keep their exact location (pixel-wise) in the visualization even when lower-index containers are removed.
     */
    @Nullable private SafeMap<Integer, String> containerPositionToId = null;

    @Override
    public HtmlString getVisualRepresentation(State state) {
        

        StringBuilder sb = new StringBuilder();

        sb.append("<div style=\"text-align: left;\">");
        sb.append("<table style=\"font-size: 2.0em;border-collapse:collapse;text-align:left;\">");

        TextFormatModification format = new TextFormatModification();


        SafeMap<Integer, String> positionToId = containerPositionToId==null ?
                generateContainerPositionToIdMap(state) : containerPositionToId;
        for(int i = 0; i < positionToId.keySet().size(); i++) {
            sb.append("<tr>");
                @Nullable ShippingContainer container = positionToId.safeContainsKey(i) ?
                        (ShippingContainer) state.getEntityById(positionToId.safeGet(i)) : null;

            String containerStr;
            if (container == null) {
                containerStr = getContainerHtmlRepresentation(format,
                        ContainerManagementHitRandomGenerator.MAX_CONTAINER_LENGTH, TextColor.WHITE);
            } else {
                TextColor color = container.contentState == ContentState.LOADED  ?  TextColor.BLUE : TextColor.GRAY;
                containerStr = getContainerHtmlRepresentation(format, container.length, color);
            }
            sb.append("<td style=\"border-style: none;border-width: 0.1em;vertical-align:left   ;padding:0.12em\"><span style=\"font-size: 0.6em\">")
                    .append(containerStr)
                    .append("</span></td>");


            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("</div>");
        return new HtmlString(sb.toString());
    }

    private String getContainerHtmlRepresentation(TextFormatModification format, int length, TextColor color) {
        double rectangleHeight = 1.0;
        double rectangleWidth = 3.0 * length;

        String str =  "<div style=\"width:" + rectangleWidth + "em;height:" + rectangleHeight +
                "em;background-color: " + color + ";\"></div>\n";

        return createHtmlFromStr(str, format,false, false);
    }



}
