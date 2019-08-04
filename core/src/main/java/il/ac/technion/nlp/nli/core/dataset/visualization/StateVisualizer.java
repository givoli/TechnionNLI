package il.ac.technion.nlp.nli.core.dataset.visualization;

import il.ac.technion.nlp.nli.core.dataset.visualization.html.HtmlString;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextColor;
import il.ac.technion.nlp.nli.core.dataset.visualization.html.TextFormatModification;
import il.ac.technion.nlp.nli.core.state.NliEntity;
import il.ac.technion.nlp.nli.core.state.State;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates a html code block which is a visualization for a given a state. Contains info regarding the way certain
 * things are to be emphasized in the visualization (e.g. specific entities).
 *
 * IMPORTANT: The fields of all descendant classes (including itself) may not reference {@link State} and may only refer
 * to {@link NliEntity} via id - so that when a {@link StateVisualizer} is valid for
 * a {@link State} it's valid for its deep copies as well.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public abstract class StateVisualizer implements Serializable{

    /**
     * Initializes a default visualizer, which is one the emphasizes nothing.
     */
    public StateVisualizer() {
    }

    private static final long serialVersionUID = -6124835668111790115L;

    /**
     * @return the visual representation of 'state', taking into account {@link #getEntityEmphasisInfo(String)} and/or
     * {@link #isEntityEntirelyEmphasized(String)} (if relevant).
     *
     * It's completely legitimate scenario when some emphasis was defined to an entityId in state, and that entity was
     * removed from 'state' before calling this method.
     */
	public abstract HtmlString getVisualRepresentation(State state);


    /**
     * If the {@link EntityEmphasisInfo} was never set for that entityId, null is returned.
     */
    protected @Nullable EntityEmphasisInfo getEntityEmphasisInfo (String entityId) {
        return entitiesEmphasisInfo.get(entityId);
    }

    /**
     * Sets the {@link EntityEmphasisInfo} of 'entityId'.
     */
    protected void setEntityEmphasisInfo (String entityId, EntityEmphasisInfo info) {
        entitiesEmphasisInfo.put(entityId,info);
    }

    protected boolean isEntityEntirelyEmphasized(String entityId) {
        return  entitiesEmphasisInfo.containsKey(entityId) &&
                entitiesEmphasisInfo.get(entityId).entireEntityIsEmphasized;

    }


    protected String createHtmlFromStr(String str, TextFormatModification format) {
        return createHtmlFromStr(str, format, false, true);
    }

    protected String createHtmlFromStr(String str, TextFormatModification format, boolean overrideColorToRed,
                                       boolean escapeHtml) {
        if (overrideColorToRed)
            format = format.deepCopy().setColor(TextColor.RED);
        return new HtmlString(str,format, escapeHtml).getString();
    }

    public void emphasizeEntireEntity(String entityId) {
        entitiesEmphasisInfo.put(entityId, new EntityEmphasisInfo(true));
    }


    /**
     * The key is an entity id.
     * An entity has a key in this map iff the entity should be emphasized in some way (either entirely or partially -
     * e.g. only some primitive field should be emphasized).
     */
    private Map<String, EntityEmphasisInfo> entitiesEmphasisInfo = new HashMap<>();


	/**
	 * Represents basic info defining in what way should an entity be emphasized in its visualization.
     * TODO: remove this class and have a simple Map<String,Boolean> map instead.
	 */
	private static class EntityEmphasisInfo implements Serializable{

        private static final long serialVersionUID = 2403040468300584850L;

        public EntityEmphasisInfo(boolean entireEntityIsEmphasized) {
            this.entireEntityIsEmphasized = entireEntityIsEmphasized;
        }

        /**
         * This field should be used only if there's additional possible info to
         * be represented by this class. Otherwise, the relevant value of the
         * {@link StateVisualizer#entitiesEmphasisInfo} map should simply be null.
         */
        boolean entireEntityIsEmphasized;

	}


}
