package il.ac.technion.nlp.nli.core.dataset.visualization.html;

import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Represent the format modification of html text relative to containing element.
 * You can nest multiple {@link TextFormatModification} formatting.
 *
 * @author Ofer Givoli <ogivoli@cs.technion.ac.il>
 */
public class TextFormatModification implements Serializable{

	private static final long serialVersionUID = 4220258483502669634L;


	public TextFormatModification() {}

    /**
     * When false - there's no modification relative to containing element.
     * Note: if the containing element is bold, the contained element is always bold as well.
     */
	private boolean bold = false;

    /**
     * When false - there's no modification relative to containing element.
     * Note: if the containing element is underlined, the contained element is always underlined as well.
     */
	private boolean underline  = false;

    /**
     * When null - there's no modification relative to containing element.
     */
    @Nullable
	private TextColor color;

    /**
     * When null - there's no modification relative to containing element.
     * Otherwise - this field defines font size modification relative to the containing element.
     */
    @Nullable
	private Double sizeModificationInCssEm;


	public TextFormatModification setBold(boolean bold) {
		this.bold = bold;
		return this;
	}

	@SuppressWarnings("UnusedReturnValue")
	public TextFormatModification setUnderline(boolean underline) {
		this.underline = underline;
		return this;
	}

	
	public TextFormatModification setColor(@Nullable TextColor color) {
		this.color = color;
		return this;
	}


	@SuppressWarnings("UnusedReturnValue")
	public TextFormatModification setSizeInCssEm(@Nullable TextSizeModification textSizeModification) {
        if (textSizeModification == null)
            this.sizeModificationInCssEm = null;
        else
            this.sizeModificationInCssEm = textSizeModification.getCssEm();

		return this;
	}

	public TextFormatModification setSizeInCssEm(@Nullable Double sizeModificationInCssEm) {
		this.sizeModificationInCssEm = sizeModificationInCssEm;
		return this;
	}

	public boolean getBold() {
		return bold;
	}

	public boolean getUnderline() {
		return underline;
	}

	public @Nullable TextColor getColor() {
		return color;
	}

	public @Nullable Double getSizeModificationInCssEm() {
		return sizeModificationInCssEm;
	}


	public TextFormatModification deepCopy() {
		return SerializationUtils.clone(this);
	}
}
