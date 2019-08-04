package il.ac.technion.nlp.nli.core.dataset.visualization.html;

import il.ac.technion.nlp.nli.core.state.entities.Color;

/**
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public enum TextColor {
    /**
     * The {@link #getHtmlColorCode()} implementation relies on the constant names to be valid html color codes
     * (ignoring case).
     */
	BLACK, BLUE, BROWN, DARKORANGE, GREEN, PURPLE, RED, SADDLEBROWN, WHITE, GRAY;

	public static TextColor fromColorEntity(Color color) {
		switch(color) { 
		case BLACK:
			return BLACK;
		case BROWN:
			return BROWN;
		case BLUE:
			return BLUE;
		case GREEN:
			return GREEN;
		case ORANGE:
			return DARKORANGE;
		case PURPLE:
			return PURPLE;
		case RED:
			return RED;
		default:
			throw new RuntimeException("the color " + color.toString() + " is not supported.");
		}
	}

	public String getHtmlColorCode(){
		return name().toLowerCase();
	}

}
